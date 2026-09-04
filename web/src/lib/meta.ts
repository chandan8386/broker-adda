import { useQuery } from '@tanstack/react-query';
import { api } from './api';
import type { CompanyMeta } from './types';

export function useCompanyMeta() {
  return useQuery({
    queryKey: ['company-meta'],
    queryFn: async () => (await api.get<CompanyMeta>('/company')).data,
    staleTime: Infinity,
  });
}
