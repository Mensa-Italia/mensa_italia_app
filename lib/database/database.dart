import 'package:mensa_italia_app/objectbox.g.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

class DB {
  static late final Store store;

  static Future<DB> init() async {
    final docsDir = await getApplicationDocumentsDirectory();
    final store = await openStore(directory: p.join(docsDir.path, "obx-mensa"));
    DB.store = store;
    return DB();
  }
}
