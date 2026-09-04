import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import 'lead_models.dart';

final leadRepositoryProvider = Provider((ref) => LeadRepository(ref));

class LeadRepository {
  LeadRepository(this._ref);
  final Ref _ref;

  Future<PageResult<LeadListItem>> list({
    int page = 0,
    String? query,
    String? status,
  }) async {
    final dio = _ref.read(apiClientProvider).dio;
    final res = await dio.get('/leads', queryParameters: {
      'page': page,
      'size': 20,
      'sort': 'createdAt,desc',
      if (query != null && query.isNotEmpty) 'q': query,
      if (status != null && status.isNotEmpty) 'status': status,
    });
    return PageResult.fromJson(res.data as Map<String, dynamic>, LeadListItem.fromJson);
  }

  Future<LeadDetail> get(int id) async {
    final dio = _ref.read(apiClientProvider).dio;
    final res = await dio.get('/leads/$id');
    return LeadDetail.fromJson(res.data as Map<String, dynamic>);
  }

  Future<List<LeadActivity>> activities(int id) async {
    final dio = _ref.read(apiClientProvider).dio;
    final res = await dio.get('/leads/$id/activities');
    return (res.data as List).map((e) => LeadActivity.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<void> transition(int id, String target, {String? note, String? lostReason, String? siteVisitAt}) async {
    final dio = _ref.read(apiClientProvider).dio;
    await dio.post('/leads/$id/transition', data: {
      'targetStatus': target,
      if (note != null) 'note': note,
      if (lostReason != null) 'lostReason': lostReason,
      if (siteVisitAt != null) 'siteVisitAt': siteVisitAt,
    });
  }

  Future<void> addNote(int id, String note) async {
    final dio = _ref.read(apiClientProvider).dio;
    await dio.post('/leads/$id/notes', data: {'note': note});
  }

  Future<void> logCall(int leadId, {required String outcome, String? disposition, String? notes, String? nextFollowUpAt}) async {
    final dio = _ref.read(apiClientProvider).dio;
    await dio.post('/calls', data: {
      'leadId': leadId,
      'outcome': outcome,
      if (disposition != null) 'disposition': disposition,
      if (notes != null) 'notes': notes,
      if (nextFollowUpAt != null) 'nextFollowUpAt': nextFollowUpAt,
    });
  }

  Future<void> createLead(Map<String, dynamic> body) async {
    final dio = _ref.read(apiClientProvider).dio;
    await dio.post('/leads', data: body);
  }
}
