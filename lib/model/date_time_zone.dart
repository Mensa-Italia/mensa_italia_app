import 'package:timezone/timezone.dart' as tz;


class RangeDateTimeZone {
  final tz.TZDateTime start;
  final tz.TZDateTime end;

  RangeDateTimeZone({required this.start, required this.end});

  RangeDateTimeZone.fromDateTimeAndZone({required DateTime start, required DateTime end, required tz.Location location})
      : start = tz.TZDateTime(location, start.year, start.month, start.day, start.hour, start.minute, start.second, start.millisecond, start.microsecond),
        end = tz.TZDateTime(location, end.year, end.month, end.day, end.hour, end.minute, end.second, end.millisecond, end.microsecond);

  RangeDateTimeZone.fromDateTime({required DateTime start, required DateTime end})
      : start = tz.TZDateTime(tz.local, start.year, start.month, start.day, start.hour, start.minute, start.second, start.millisecond, start.microsecond),
        end = tz.TZDateTime(tz.local, end.year, end.month, end.day, end.hour, end.minute, end.second, end.millisecond, end.microsecond);
}
