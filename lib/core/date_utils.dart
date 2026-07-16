import 'package:intl/intl.dart';

final DateFormat _storageDateFormat = DateFormat('yyyy-MM-dd');
final DateFormat _monthDayFormat = DateFormat('M月d日');
final DateFormat _monthFormat = DateFormat('yyyy年M月');

String dateKey(DateTime date) => _storageDateFormat.format(date);

DateTime dateFromKey(String value) => DateTime.parse(value);

DateTime dateOnly(DateTime value) =>
    DateTime(value.year, value.month, value.day);

bool isSameDate(DateTime left, DateTime right) =>
    left.year == right.year &&
    left.month == right.month &&
    left.day == right.day;

String friendlyDate(DateTime date) {
  final today = dateOnly(DateTime.now());
  final target = dateOnly(date);
  final difference = target.difference(today).inDays;
  final prefix = switch (difference) {
    0 => '今天',
    -1 => '昨天',
    1 => '明天',
    _ => '',
  };
  const weekdays = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'];
  final detail =
      '${_monthDayFormat.format(target)} ${weekdays[target.weekday - 1]}';
  return prefix.isEmpty ? detail : '$prefix · $detail';
}

String monthLabel(DateTime date) => _monthFormat.format(date);
