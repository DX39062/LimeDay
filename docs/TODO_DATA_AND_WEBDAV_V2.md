# LimeDay 2.7 待办领域模型与 WebDAV v2

## 设计原则

2.7 继续以“每天先计划、之后复盘”为主线，不把应用改造成复杂项目管理器。新增字段参考成熟待办工具的截止、提醒、重复、步骤与清单能力，但只实现一级分组，不加入附件、协作、标签或多级项目树。

`date` 始终表示计划日。截止日期、截止时间和提醒是附加约束，不能通过移动 Todo 的 `date` 来模拟。这样同一事项可以在 7 月 20 日进入计划，但在 7 月 25 日 18:00 截止。

## Todo 扩展字段

- `groupId`：TodoGroup UUID；旧数据迁入内置收件箱。
- `dueDate`：可选 ISO 日期。
- `dueTime`：可选 `HH:mm`；只有日期时保持 null。
- `dueAt`：同时有日期和时间时计算的 UTC 毫秒。
- `dueZoneId`：计算时区；跨时区后截止瞬间不漂移，界面可按当前时区显示。
- `reminderAt`：独立提醒 UTC 毫秒。
- `recurrence`：版本化字符串；首版为 `none`、`daily`、`weekdays`、`weekly`、`monthly`、`interval:N:DAYS|WEEKS|MONTHS`。
- `recurrenceSourceId`：重复链 ID；首实例为空时使用自身 ID，后续实例沿用。

逾期条件为有效、未完成、`dueDate` 早于今天，或 `dueAt` 早于当前时间。只有计划日期没有截止日期的 Todo 永不因计划日过去自动成为逾期。

## 重复实例

完成有重复规则的 Todo 时，在同一数据库事务中：

1. 写入当前实例完成状态。
2. 按规则计算严格晚于当前计划/截止基准的下一日期。
3. 检查同一 `recurrenceSourceId` 与目标日期是否已有实例，保证幂等。
4. 创建新 UUID、未完成的新实例，复制标题、备注、优先级、分组、截止本地时间、提醒相对偏移和步骤标题；步骤完成状态重置。
5. 调度新实例提醒，取消当前实例提醒。

取消完成不会自动删除已经生成的下一实例，避免跨设备同步反复创建/删除；界面需要说明这一规则。

## 分组

TodoGroup 是同步实体，包含名称、`iconKey`、`colorKey`、`sortOrder`、`isInbox` 和同步元数据。每个安装必须存在一个固定 ID 的内置“收件箱”。用户不能删除收件箱。

删除普通分组采用软删除。其 Todo 不批量改写，读取与显示时将无效 groupId 解析为收件箱；用户之后编辑 Todo 时可写回收件箱 ID。这样删除分组不会制造大量同步冲突。

待办页按有效分组顺序显示区段，折叠状态只保存在本机 UI 状态。逾期、计划中和搜索结果可以按分组继续分区。

## 步骤

TodoStep 是独立同步实体，包含 `todoId`、标题、完成状态、排序与同步元数据。删除步骤采用软删除；主 Todo 软删除时步骤保留以支持恢复，主 Todo 永久删除时步骤正文物理清除。

搜索在 Room 中覆盖 Todo 标题/备注和有效步骤标题。首版不实现全文索引，以转义后的 `LIKE` 查询满足本地规模；结果按逾期、未完成、计划日和稳定排序排列。

## 单项提醒

提醒使用 WorkManager 唯一任务 `todo-reminder-<todoId>`。它是尽力而为的用户提醒，不申请 Android 特殊精确闹钟权限。调度输入只保存 Todo ID；Worker 执行时重新读取数据库，只有 Todo 仍有效、未完成且 `reminderAt` 与任务匹配时才通知。

新增或编辑重排任务；完成、软删除、永久删除取消任务；恢复时仅当提醒仍在未来才重新调度。设备重启由 WorkManager 自恢复。通知点击打开应用并选中 Todo 计划日。

## 永久删除与墓碑

软删除 Todo 仍保留完整内容并在回收站可恢复。永久删除执行事务：

1. 用 Todo 当前版本生成 `TodoTombstone(todoId, deletedAt, updatedAt, deviceId, revision)`，revision 至少比正文大 1。
2. 物理删除该 Todo 的步骤与 Todo 正文。
3. 取消单项提醒。

墓碑永不显示且当前版本不自动清理。同步合并先分别合并 Todo 与 Tombstone，再以版本比较过滤：墓碑不旧于 Todo 时丢弃 Todo；Todo 更新版本严格更新时允许显式恢复跨越墓碑。普通恢复必须递增到大于墓碑的版本。

## WebDAV JSON v2

默认文件名为 `limeday-sync-v2.json`，顶层结构：

```json
{
  "formatVersion": 2,
  "generatedAt": 0,
  "deviceId": "uuid",
  "todos": [],
  "groups": [],
  "steps": [],
  "todoTombstones": [],
  "reviews": [],
  "summaries": [],
  "rangeSummaries": []
}
```

v2 Todo 序列化全部扩展字段。Group、Step 与 Tombstone 使用 `updatedAt`、`revision`、`deviceId` 同样决胜。解析必须先完整构造快照再进入事务，任何字段类型错误或未知 `formatVersion` 都不得部分写入。

升级同步流程：

1. 首先请求 v2 文件。
2. v2 为 404 时请求旧 v1 文件。
3. v1 存在则解析为内存 v2：生成/复用收件箱，所有 Todo 指向收件箱，新集合为空。
4. 与本地 v2 合并后写入 v2 文件；不再覆盖 v1，给旧客户端留下只读旧快照并阻止其误解 v2。
5. v2 一旦存在，后续不再读取 v1。

导入可接受 v1 或 v2，导出只产生 v2。供应商、端点、API Key、WebDAV 密码、模型缓存和 AppSettings 均不进入快照。
