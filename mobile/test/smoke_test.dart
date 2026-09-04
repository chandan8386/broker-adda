import 'package:flutter_test/flutter_test.dart';
import 'package:trishakti_crm/features/leads/lead_models.dart';

void main() {
  test('PageResult parses content and paging flags', () {
    final json = {
      'content': [
        {'id': 1, 'reference': 'LD-000001', 'customerName': 'Ramesh', 'mobile': '9812345678', 'status': 'NEW'},
      ],
      'page': 0,
      'totalPages': 3,
      'last': false,
    };
    final page = PageResult.fromJson(json, LeadListItem.fromJson);
    expect(page.content.single.customerName, 'Ramesh');
    expect(page.last, isFalse);
    expect(page.totalPages, 3);
  });

  test('LeadDetail exposes typed getters over the raw map', () {
    final d = LeadDetail.fromJson({
      'id': 5,
      'reference': 'LD-000005',
      'customerName': 'Sunita',
      'mobile': '9822233344',
      'status': 'INTERESTED',
      'budgetMin': 4000000,
      'budgetMax': 6000000,
    });
    expect(d.status, 'INTERESTED');
    expect(d.budgetMax, 6000000);
    expect(d.email, isNull);
  });
}
