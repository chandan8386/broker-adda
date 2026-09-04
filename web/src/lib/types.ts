export type Role = 'ADMIN' | 'SALES_MANAGER' | 'CALLING_TEAM' | 'SALES_EXECUTIVE';

export type LeadStatus =
  | 'NEW' | 'ASSIGNED' | 'CALLING' | 'CONNECTED' | 'NOT_CONNECTED' | 'INTERESTED'
  | 'NOT_INTERESTED' | 'SITE_VISIT_SCHEDULED' | 'SITE_VISIT_DONE' | 'NEGOTIATION'
  | 'BOOKING' | 'PURCHASED' | 'CLOSED' | 'LOST';

export interface UserSummary {
  id: number; fullName: string; username: string; email: string;
  phone?: string; roles: Role[]; team?: string;
}

export interface AuthResponse {
  accessToken: string; refreshToken: string; tokenType: string;
  expiresInMs: number; user: UserSummary;
}

export interface Page<T> {
  content: T[]; page: number; size: number; totalElements: number;
  totalPages: number; first: boolean; last: boolean; empty: boolean;
}

export interface LeadListItem {
  id: number; reference: string; customerName: string; mobile: string;
  sourceChannel?: string; propertyType?: string; status: LeadStatus;
  priority?: string; assignedUserName?: string; nextFollowUpAt?: string; createdAt: string;
}

export interface LeadResponse extends LeadListItem {
  email?: string; source?: string; budgetMin?: number; budgetMax?: number;
  preferredLocation?: string; assignedUserId?: number; interestedPropertyId?: number;
  interestedPropertyTitle?: string; notes?: string; lostReason?: string;
  lastContactedAt?: string; updatedAt?: string; createdBy?: string;
}

export interface Activity {
  id: number; type: string; fromStatus?: LeadStatus; toStatus?: LeadStatus;
  summary: string; detail?: string; actorName: string; occurredAt: string;
}

export interface DashboardSummary {
  totalLeads: number; newLeads: number; todaysFollowUps: number; overdueFollowUps: number;
  interestedLeads: number; siteVisitsScheduled: number; siteVisitsCompleted: number;
  bookings: number; purchases: number; lostLeads: number; leadConversionPercent: number;
  leadsByStatus: Record<string, number>; totalSalesValue: number;
}

export interface ReportsResponse {
  sourceWise: { source: string; totalLeads: number; converted: number; conversionPercent: number }[];
  callingTeam: { userId: number; name: string; totalCalls: number; connected: number; interested: number; siteVisits: number; connectRate: number }[];
  salesTeam: { userId: number; name: string; assignedLeads: number; interested: number; purchased: number; closeRate: number }[];
}

export interface SiteVisit {
  id: number; leadId: number; leadName: string; leadMobile: string;
  propertyId?: number; propertyTitle?: string; salesExecutiveId?: number; salesExecutiveName?: string;
  scheduledAt: string; status: string; result?: string; feedback?: string;
  pickupRequired: boolean; rating?: number; completedAt?: string; createdAt: string;
}

export interface Property {
  id: number; projectId?: number; projectName?: string; title: string; propertyType: string;
  unitNumber?: string; location?: string; city?: string; areaSqft?: number; price?: number;
  bedrooms?: number; bathrooms?: number; facing?: string; status: string; description?: string;
}

export interface NotificationItem {
  id: number; type: string; title: string; body?: string;
  entityType?: string; entityId?: number; read: boolean; readAt?: string; createdAt: string;
}

export interface CompanyMeta {
  name: string;
  product: string;
  enums: Record<string, string[]>;
  leadWorkflow: Record<string, string[]>;
}
