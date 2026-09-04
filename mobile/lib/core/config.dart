class AppConfig {
  /// Override at build/run time:
  ///   flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api',
  );

  static const String companyName = 'Shri Trishakti Infra Realtors Pvt Ltd';
}
