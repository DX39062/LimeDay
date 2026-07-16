import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/date_utils.dart';
import '../../data/local/app_database.dart';
import '../../domain/llm_config.dart';
import '../../llm/llm_client.dart';
import '../../providers.dart';

class DayPage extends ConsumerWidget {
  const DayPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedDate = ref.watch(selectedDateProvider);
    final key = dateKey(selectedDate);
    final todosAsync = ref.watch(todosProvider(key));
    final reviewAsync = ref.watch(reviewProvider(key));
    final summaryAsync = ref.watch(summaryProvider(key));

    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        slivers: [
          SliverToBoxAdapter(
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 780),
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 22, 20, 48),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _DayHeader(date: selectedDate),
                      const SizedBox(height: 20),
                      _DateNavigator(date: selectedDate),
                      const SizedBox(height: 22),
                      todosAsync.when(
                        data: (todos) => Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            _ProgressPanel(todos: todos),
                            const SizedBox(height: 30),
                            _TodoSection(date: selectedDate, todos: todos),
                          ],
                        ),
                        loading: () => const _LoadingBlock(height: 220),
                        error: (error, stack) =>
                            const _ErrorBlock(message: '待办读取失败，请重新打开应用'),
                      ),
                      const SizedBox(height: 34),
                      _SectionTitle(
                        title: '每日复盘',
                        icon: Icons.edit_note_rounded,
                      ),
                      const SizedBox(height: 14),
                      reviewAsync.when(
                        data: (review) => ReviewEditor(
                          key: ValueKey(key),
                          date: selectedDate,
                          review: review,
                        ),
                        loading: () => const _LoadingBlock(height: 320),
                        error: (error, stack) =>
                            const _ErrorBlock(message: '复盘读取失败，请重新打开应用'),
                      ),
                      const SizedBox(height: 34),
                      _SectionTitle(
                        title: '智能总结',
                        icon: Icons.auto_awesome_rounded,
                      ),
                      const SizedBox(height: 14),
                      _SummarySection(
                        date: selectedDate,
                        todos: todosAsync.value ?? const [],
                        review: reviewAsync.value,
                        summary: summaryAsync.value,
                        loading: summaryAsync.isLoading,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DayHeader extends StatelessWidget {
  const _DayHeader({required this.date});
  final DateTime date;

  @override
  Widget build(BuildContext context) {
    final today = isSameDate(date, DateTime.now());
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(
              Icons.spa_rounded,
              size: 22,
              color: Theme.of(context).colorScheme.primary,
            ),
            const SizedBox(width: 8),
            Text(
              '青柠日记',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Text(
          today ? '今天，也要轻盈前行' : '回望这一天',
          style: Theme.of(context).textTheme.headlineMedium,
        ),
        const SizedBox(height: 6),
        Text(
          friendlyDate(date),
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

class _DateNavigator extends ConsumerWidget {
  const _DateNavigator({required this.date});
  final DateTime date;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = ref.read(selectedDateProvider.notifier);
    final today = isSameDate(date, DateTime.now());
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          IconButton(
            tooltip: '前一天',
            onPressed: controller.previous,
            icon: const Icon(Icons.chevron_left_rounded),
          ),
          Expanded(
            child: TextButton.icon(
              onPressed: () async {
                final selected = await showDatePicker(
                  context: context,
                  initialDate: date,
                  firstDate: DateTime(2000),
                  lastDate: DateTime(2100),
                  helpText: '选择日期',
                  cancelText: '取消',
                  confirmText: '确定',
                );
                if (selected != null) controller.select(selected);
              },
              icon: const Icon(Icons.calendar_today_outlined, size: 18),
              label: Text(friendlyDate(date)),
            ),
          ),
          if (!today)
            IconButton(
              tooltip: '回到今天',
              onPressed: controller.today,
              icon: const Icon(Icons.my_location_rounded),
            ),
          IconButton(
            tooltip: '后一天',
            onPressed: controller.next,
            icon: const Icon(Icons.chevron_right_rounded),
          ),
        ],
      ),
    );
  }
}

class _ProgressPanel extends StatelessWidget {
  const _ProgressPanel({required this.todos});
  final List<TodoItem> todos;

  @override
  Widget build(BuildContext context) {
    final complete = todos.where((todo) => todo.isCompleted).length;
    final progress = todos.isEmpty ? 0.0 : complete / todos.length;
    final percent = (progress * 100).round();
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      label: '今日进度，已完成$complete项，共${todos.length}项，$percent%',
      child: Card(
        color: scheme.primaryContainer,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '今日进度',
                          style: Theme.of(context).textTheme.labelLarge,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '$complete / ${todos.length} 项完成',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ],
                    ),
                  ),
                  Text(
                    '$percent%',
                    style: Theme.of(
                      context,
                    ).textTheme.headlineSmall?.copyWith(color: scheme.primary),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              LinearProgressIndicator(
                value: progress,
                minHeight: 8,
                borderRadius: BorderRadius.circular(4),
                backgroundColor: scheme.surface.withValues(alpha: 0.6),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TodoSection extends ConsumerWidget {
  const _TodoSection({required this.date, required this.todos});
  final DateTime date;
  final List<TodoItem> todos;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final repository = ref.read(repositoryProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            const Expanded(
              child: _SectionTitle(
                title: '每日待办',
                icon: Icons.checklist_rounded,
              ),
            ),
            FilledButton.icon(
              onPressed: () => _showTodoEditor(
                context,
                onSave: (title, note) =>
                    repository.addTodo(date, title, note: note),
              ),
              icon: const Icon(Icons.add_rounded),
              label: const Text('添加'),
            ),
          ],
        ),
        const SizedBox(height: 14),
        if (todos.isEmpty)
          Container(
            padding: const EdgeInsets.symmetric(vertical: 34, horizontal: 20),
            decoration: BoxDecoration(
              border: Border.all(
                color: Theme.of(context).colorScheme.outlineVariant,
              ),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Column(
              children: [
                Icon(
                  Icons.task_alt_rounded,
                  size: 36,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(height: 10),
                Text('今天还没有待办', style: Theme.of(context).textTheme.titleMedium),
              ],
            ),
          )
        else
          AnimatedSize(
            duration: const Duration(milliseconds: 220),
            child: Column(
              children: [
                for (final todo in todos) ...[
                  _TodoTile(
                    key: ValueKey(todo.id),
                    todo: todo,
                    onToggle: () => repository.toggleTodo(todo),
                    onEdit: () => _showTodoEditor(
                      context,
                      todo: todo,
                      onSave: (title, note) =>
                          repository.updateTodo(todo, title, note),
                    ),
                    onDelete: () => repository.deleteTodo(todo),
                  ),
                  if (todo != todos.last) const SizedBox(height: 8),
                ],
              ],
            ),
          ),
      ],
    );
  }
}

class _TodoTile extends StatelessWidget {
  const _TodoTile({
    super.key,
    required this.todo,
    required this.onToggle,
    required this.onEdit,
    required this.onDelete,
  });

  final TodoItem todo;
  final VoidCallback onToggle;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onEdit,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(6, 8, 4, 8),
          child: Row(
            children: [
              Semantics(
                button: true,
                checked: todo.isCompleted,
                label: todo.isCompleted
                    ? '取消完成：${todo.title}'
                    : '标记完成：${todo.title}',
                child: IconButton(
                  onPressed: onToggle,
                  icon: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 180),
                    child: Icon(
                      todo.isCompleted
                          ? Icons.check_circle_rounded
                          : Icons.radio_button_unchecked_rounded,
                      key: ValueKey(todo.isCompleted),
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                ),
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      todo.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        decoration: todo.isCompleted
                            ? TextDecoration.lineThrough
                            : null,
                        color: todo.isCompleted
                            ? Theme.of(context).colorScheme.onSurfaceVariant
                            : null,
                      ),
                    ),
                    if (todo.note.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        todo.note,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              PopupMenuButton<String>(
                tooltip: '待办操作',
                onSelected: (value) {
                  if (value == 'edit') onEdit();
                  if (value == 'delete') onDelete();
                },
                itemBuilder: (context) => const [
                  PopupMenuItem(
                    value: 'edit',
                    child: ListTile(
                      leading: Icon(Icons.edit_outlined),
                      title: Text('编辑'),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                  PopupMenuItem(
                    value: 'delete',
                    child: ListTile(
                      leading: Icon(Icons.delete_outline),
                      title: Text('删除'),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

Future<void> _showTodoEditor(
  BuildContext context, {
  TodoItem? todo,
  required Future<void> Function(String title, String note) onSave,
}) async {
  final titleController = TextEditingController(text: todo?.title ?? '');
  final noteController = TextEditingController(text: todo?.note ?? '');
  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (context) => Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        4,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            todo == null ? '添加待办' : '编辑待办',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 18),
          TextField(
            controller: titleController,
            autofocus: true,
            maxLength: 80,
            textInputAction: TextInputAction.next,
            decoration: const InputDecoration(labelText: '标题'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: noteController,
            maxLength: 300,
            minLines: 2,
            maxLines: 4,
            decoration: const InputDecoration(labelText: '备注（可选）'),
          ),
          const SizedBox(height: 14),
          FilledButton(
            onPressed: () async {
              if (titleController.text.trim().isEmpty) return;
              await onSave(titleController.text, noteController.text);
              if (context.mounted) Navigator.pop(context);
            },
            child: const Text('保存'),
          ),
        ],
      ),
    ),
  );
  titleController.dispose();
  noteController.dispose();
}

class ReviewEditor extends ConsumerStatefulWidget {
  const ReviewEditor({super.key, required this.date, required this.review});

  final DateTime date;
  final DailyReview? review;

  @override
  ConsumerState<ReviewEditor> createState() => _ReviewEditorState();
}

class _ReviewEditorState extends ConsumerState<ReviewEditor>
    with WidgetsBindingObserver {
  late final TextEditingController _highlight;
  late final TextEditingController _challenge;
  late final TextEditingController _learning;
  late final TextEditingController _tomorrow;
  Timer? _debounce;
  int _mood = 0;
  bool _dirty = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _highlight = TextEditingController(text: widget.review?.highlight ?? '');
    _challenge = TextEditingController(text: widget.review?.challenge ?? '');
    _learning = TextEditingController(text: widget.review?.learning ?? '');
    _tomorrow = TextEditingController(text: widget.review?.tomorrowFocus ?? '');
    _mood = widget.review?.mood ?? 0;
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      _saveNow();
    }
  }

  void _changed() {
    _dirty = true;
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), _saveNow);
  }

  Future<void> _saveNow() async {
    if (!_dirty) return;
    _dirty = false;
    await ref
        .read(repositoryProvider)
        .saveReview(
          date: widget.date,
          highlight: _highlight.text,
          challenge: _challenge.text,
          learning: _learning.text,
          tomorrowFocus: _tomorrow.text,
          mood: _mood,
        );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _debounce?.cancel();
    _saveNow();
    _highlight.dispose();
    _challenge.dispose();
    _learning.dispose();
    _tomorrow.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _ReviewField(
          label: '今天最值得记住的亮点',
          controller: _highlight,
          onChanged: _changed,
        ),
        const SizedBox(height: 12),
        _ReviewField(
          label: '遇到了什么困难？',
          controller: _challenge,
          onChanged: _changed,
        ),
        const SizedBox(height: 12),
        _ReviewField(
          label: '今天有什么收获？',
          controller: _learning,
          onChanged: _changed,
        ),
        const SizedBox(height: 12),
        _ReviewField(
          label: '明天最重要的一件事',
          controller: _tomorrow,
          onChanged: _changed,
        ),
        const SizedBox(height: 18),
        Text('今天的心情', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final entry in const {
              1: '很累',
              2: '低落',
              3: '平静',
              4: '不错',
              5: '闪亮',
            }.entries)
              ChoiceChip(
                label: Text(entry.value),
                selected: _mood == entry.key,
                onSelected: (selected) {
                  setState(() => _mood = selected ? entry.key : 0);
                  _changed();
                },
              ),
          ],
        ),
      ],
    );
  }
}

class _ReviewField extends StatelessWidget {
  const _ReviewField({
    required this.label,
    required this.controller,
    required this.onChanged,
  });
  final String label;
  final TextEditingController controller;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      onChanged: (_) => onChanged(),
      maxLength: 1000,
      minLines: 2,
      maxLines: 5,
      textCapitalization: TextCapitalization.sentences,
      decoration: InputDecoration(labelText: label, counterText: ''),
    );
  }
}

class _SummarySection extends ConsumerStatefulWidget {
  const _SummarySection({
    required this.date,
    required this.todos,
    required this.review,
    required this.summary,
    required this.loading,
  });

  final DateTime date;
  final List<TodoItem> todos;
  final DailyReview? review;
  final DailySummary? summary;
  final bool loading;

  @override
  ConsumerState<_SummarySection> createState() => _SummarySectionState();
}

class _SummarySectionState extends ConsumerState<_SummarySection> {
  bool _generating = false;
  String? _error;
  CancelToken? _cancelToken;

  Future<void> _generate() async {
    final config = await ref.read(llmConfigStoreProvider).load();
    if (!mounted) return;
    if (!config.isConfigured) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('请先在“设置”中完成模型配置')));
      }
      return;
    }
    if (widget.todos.isEmpty && _reviewIsEmpty(widget.review)) {
      setState(() => _error = '请先记录待办或复盘内容');
      return;
    }
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('发送当日记录？'),
        content: Text('待办与复盘内容将发送给 ${config.provider.displayName}。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('继续'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    _cancelToken = CancelToken();
    setState(() {
      _generating = true;
      _error = null;
    });
    try {
      final content = await ref
          .read(llmClientProvider)
          .summarize(config, _buildPrompt(), cancelToken: _cancelToken);
      await ref
          .read(repositoryProvider)
          .saveSummary(
            date: widget.date,
            content: content,
            provider: config.provider.id,
            model: config.model,
          );
    } on LlmException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _generating = false);
      _cancelToken = null;
    }
  }

  String _buildPrompt() {
    final complete = widget.todos.where((todo) => todo.isCompleted).length;
    final review = widget.review;
    final buffer = StringBuffer()
      ..writeln('日期：${dateKey(widget.date)}')
      ..writeln('待办完成：$complete/${widget.todos.length}')
      ..writeln('待办记录：');
    if (widget.todos.isEmpty) buffer.writeln('（无）');
    for (final todo in widget.todos) {
      buffer.writeln('- [${todo.isCompleted ? '已完成' : '未完成'}] ${todo.title}');
    }
    buffer
      ..writeln('复盘记录：')
      ..writeln('今日亮点：${_orEmpty(review?.highlight)}')
      ..writeln('困难：${_orEmpty(review?.challenge)}')
      ..writeln('收获：${_orEmpty(review?.learning)}')
      ..writeln('明日重点：${_orEmpty(review?.tomorrowFocus)}')
      ..writeln(
        '心情评分：${review?.mood == null || review!.mood == 0 ? '未选择' : '${review.mood}/5'}',
      );
    return buffer.toString();
  }

  String _orEmpty(String? value) =>
      value == null || value.trim().isEmpty ? '（未填写）' : value;

  bool _reviewIsEmpty(DailyReview? review) =>
      review == null ||
      (review.highlight.isEmpty &&
          review.challenge.isEmpty &&
          review.learning.isEmpty &&
          review.tomorrowFocus.isEmpty &&
          review.mood == 0);

  @override
  Widget build(BuildContext context) {
    if (widget.loading) return const _LoadingBlock(height: 140);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (widget.summary != null)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        Icons.auto_awesome_rounded,
                        size: 20,
                        color: Theme.of(context).colorScheme.tertiary,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          '${LlmProvider.fromId(widget.summary!.provider).displayName} · '
                          '${widget.summary!.model}',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.labelLarge,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  SelectableText(widget.summary!.content),
                ],
              ),
            ),
          ),
        if (_error != null) ...[
          if (widget.summary != null) const SizedBox(height: 10),
          Text(
            _error!,
            style: TextStyle(color: Theme.of(context).colorScheme.error),
          ),
        ],
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                onPressed: _generating ? null : _generate,
                icon: _generating
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.auto_awesome_rounded),
                label: Text(widget.summary == null ? '生成总结' : '重新生成'),
              ),
            ),
            if (_generating) ...[
              const SizedBox(width: 8),
              IconButton.filledTonal(
                tooltip: '取消生成',
                onPressed: () => _cancelToken?.cancel(),
                icon: const Icon(Icons.stop_rounded),
              ),
            ],
          ],
        ),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title, required this.icon});
  final String title;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 22, color: Theme.of(context).colorScheme.primary),
        const SizedBox(width: 9),
        Text(title, style: Theme.of(context).textTheme.titleLarge),
      ],
    );
  }
}

class _LoadingBlock extends StatelessWidget {
  const _LoadingBlock({required this.height});
  final double height;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      child: const Center(child: CircularProgressIndicator()),
    );
  }
}

class _ErrorBlock extends StatelessWidget {
  const _ErrorBlock({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: Theme.of(context).colorScheme.errorContainer,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.error_outline_rounded),
            const SizedBox(width: 10),
            Expanded(child: Text(message)),
          ],
        ),
      ),
    );
  }
}
