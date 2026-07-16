import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/date_utils.dart';
import '../../providers.dart';

class CalendarPage extends ConsumerStatefulWidget {
  const CalendarPage({super.key, required this.onOpenDate});
  final VoidCallback onOpenDate;

  @override
  ConsumerState<CalendarPage> createState() => _CalendarPageState();
}

class _CalendarPageState extends ConsumerState<CalendarPage> {
  late DateTime _month;

  @override
  void initState() {
    super.initState();
    final selected = ref.read(selectedDateProvider);
    _month = DateTime(selected.year, selected.month);
  }

  void _moveMonth(int offset) {
    setState(() => _month = DateTime(_month.year, _month.month + offset));
  }

  @override
  Widget build(BuildContext context) {
    final selected = ref.watch(selectedDateProvider);
    final activeDates =
        ref.watch(activeDatesProvider).value ?? const <String>{};
    return SafeArea(
      bottom: false,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 820),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 26, 20, 48),
            children: [
              Text('日历', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 6),
              Text(
                '选择一天',
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 26),
              Row(
                children: [
                  IconButton(
                    tooltip: '上个月',
                    onPressed: () => _moveMonth(-1),
                    icon: const Icon(Icons.chevron_left_rounded),
                  ),
                  Expanded(
                    child: Text(
                      monthLabel(_month),
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    tooltip: '下个月',
                    onPressed: () => _moveMonth(1),
                    icon: const Icon(Icons.chevron_right_rounded),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              _MonthGrid(
                month: _month,
                selectedDate: selected,
                activeDates: activeDates,
                onSelect: (date) {
                  ref.read(selectedDateProvider.notifier).select(date);
                  widget.onOpenDate();
                },
              ),
              const SizedBox(height: 24),
              OutlinedButton.icon(
                onPressed: () {
                  final today = dateOnly(DateTime.now());
                  ref.read(selectedDateProvider.notifier).select(today);
                  setState(() => _month = DateTime(today.year, today.month));
                  widget.onOpenDate();
                },
                icon: const Icon(Icons.my_location_rounded),
                label: const Text('回到今天'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MonthGrid extends StatelessWidget {
  const _MonthGrid({
    required this.month,
    required this.selectedDate,
    required this.activeDates,
    required this.onSelect,
  });

  final DateTime month;
  final DateTime selectedDate;
  final Set<String> activeDates;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    final first = DateTime(month.year, month.month);
    final firstOffset = first.weekday - 1;
    final days = DateUtils.getDaysInMonth(month.year, month.month);
    final totalCells = ((firstOffset + days + 6) ~/ 7) * 7;
    const weekdayLabels = ['一', '二', '三', '四', '五', '六', '日'];
    return Column(
      children: [
        GridView.count(
          crossAxisCount: 7,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          childAspectRatio: 1.4,
          children: [
            for (final label in weekdayLabels)
              Center(
                child: Text(
                  label,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
          ],
        ),
        GridView.builder(
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 7,
            childAspectRatio: 1,
            mainAxisSpacing: 4,
            crossAxisSpacing: 4,
          ),
          itemCount: totalCells,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemBuilder: (context, index) {
            final day = index - firstOffset + 1;
            if (day < 1 || day > days) return const SizedBox.shrink();
            final date = DateTime(month.year, month.month, day);
            return _DayCell(
              date: date,
              selected: isSameDate(date, selectedDate),
              today: isSameDate(date, DateTime.now()),
              hasContent: activeDates.contains(dateKey(date)),
              onTap: () => onSelect(date),
            );
          },
        ),
      ],
    );
  }
}

class _DayCell extends StatelessWidget {
  const _DayCell({
    required this.date,
    required this.selected,
    required this.today,
    required this.hasContent,
    required this.onTap,
  });
  final DateTime date;
  final bool selected;
  final bool today;
  final bool hasContent;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      button: true,
      selected: selected,
      label: '${friendlyDate(date)}${hasContent ? '，有记录' : ''}',
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Container(
          decoration: BoxDecoration(
            color: selected ? scheme.primaryContainer : null,
            borderRadius: BorderRadius.circular(10),
            border: today && !selected
                ? Border.all(color: scheme.primary)
                : null,
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '${date.day}',
                style: TextStyle(
                  fontWeight: selected || today ? FontWeight.w700 : null,
                  color: selected ? scheme.onPrimaryContainer : null,
                ),
              ),
              const SizedBox(height: 4),
              SizedBox.square(
                dimension: 5,
                child: hasContent
                    ? DecoratedBox(
                        decoration: BoxDecoration(
                          color: selected ? scheme.primary : scheme.tertiary,
                          shape: BoxShape.circle,
                        ),
                      )
                    : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
