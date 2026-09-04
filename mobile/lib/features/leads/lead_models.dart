class PageResult<T> {
  PageResult({required this.content, required this.page, required this.totalPages, required this.last});
  final List<T> content;
  final int page;
  final int totalPages;
  final bool last;

  factory PageResult.fromJson(Map<String, dynamic> j, T Function(Map<String, dynamic>) item) => PageResult(
        content: (j['content'] as List).map((e) => item(e as Map<String, dynamic>)).toList(),
        page: j['page'] as int? ?? 0,
        totalPages: j['totalPages'] as int? ?? 1,
        last: j['last'] as bool? ?? true,
      );
}

class LeadListItem {
  LeadListItem({
    required this.id,
    required this.reference,
    required this.customerName,
    required this.mobile,
    required this.status,
    this.priority,
    this.sourceChannel,
    this.propertyType,
    this.assignedUserName,
    this.nextFollowUpAt,
  });

  final int id;
  final String reference;
  final String customerName;
  final String mobile;
  final String status;
  final String? priority;
  final String? sourceChannel;
  final String? propertyType;
  final String? assignedUserName;
  final String? nextFollowUpAt;

  factory LeadListItem.fromJson(Map<String, dynamic> j) => LeadListItem(
        id: j['id'] as int,
        reference: j['reference'] as String,
        customerName: j['customerName'] as String,
        mobile: j['mobile'] as String,
        status: j['status'] as String,
        priority: j['priority'] as String?,
        sourceChannel: j['sourceChannel'] as String?,
        propertyType: j['propertyType'] as String?,
        assignedUserName: j['assignedUserName'] as String?,
        nextFollowUpAt: j['nextFollowUpAt'] as String?,
      );
}

class LeadActivity {
  LeadActivity({required this.summary, this.detail, required this.actorName, required this.occurredAt});
  final String summary;
  final String? detail;
  final String actorName;
  final String occurredAt;

  factory LeadActivity.fromJson(Map<String, dynamic> j) => LeadActivity(
        summary: j['summary'] as String,
        detail: j['detail'] as String?,
        actorName: j['actorName'] as String? ?? 'System',
        occurredAt: j['occurredAt'] as String,
      );
}

class LeadDetail {
  LeadDetail({required this.raw});
  final Map<String, dynamic> raw;

  int get id => raw['id'] as int;
  String get reference => raw['reference'] as String;
  String get customerName => raw['customerName'] as String;
  String get mobile => raw['mobile'] as String;
  String? get email => raw['email'] as String?;
  String get status => raw['status'] as String;
  String? get priority => raw['priority'] as String?;
  String? get source => raw['source'] as String?;
  String? get preferredLocation => raw['preferredLocation'] as String?;
  String? get assignedUserName => raw['assignedUserName'] as String?;
  String? get nextFollowUpAt => raw['nextFollowUpAt'] as String?;
  String? get notes => raw['notes'] as String?;
  num? get budgetMin => raw['budgetMin'] as num?;
  num? get budgetMax => raw['budgetMax'] as num?;

  factory LeadDetail.fromJson(Map<String, dynamic> j) => LeadDetail(raw: j);
}
