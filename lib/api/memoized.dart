class Memoized {
  static final Memoized _instance = Memoized._internal();
  factory Memoized() => _instance;
  Memoized._internal();
  final Map<String, dynamic> _cache = {};

  void set(String key, dynamic value) {
    _cache[key] = value;
  }

  dynamic get(String key) {
    return _cache[key];
  }

  bool has(String key) {
    return _cache.containsKey(key);
  }

  void clear() {
    _cache.clear();
  }

  void remove(String s) {
    _cache.remove(s);
  }
}
