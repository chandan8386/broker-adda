import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';

final dashboardProvider = FutureProvider.autoDispose((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/dashboard/summary');
  return DashboardSummary.fromJson(res.data as Map<String, dynamic>);
});

class DashboardSummary {
  DashboardSummary(this.raw);
  final Map<String, dynamic> raw;

  int f(String k) => (raw[k] as num?)?.toInt() ?? 0;
  double get conversion => (raw['leadConversionPercent'] as num?)?.toDouble() ?? 0;
  num get salesValue => raw['totalSalesValue'] as num? ?? 0;

  factory DashboardSummary.fromJson(Map<String, dynamic> j) => DashboardSummary(j);
}
