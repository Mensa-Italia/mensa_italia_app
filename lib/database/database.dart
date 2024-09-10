import 'package:isar/isar.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

class DB {
  static Isar? _isar;
  static Isar get isar => _isar!;
  static init() async {
    final dir = await getApplicationDocumentsDirectory();
    DB._isar = await Isar.open(
      [RegSociModelSchema],
      directory: dir.path,
    );
  }
}
