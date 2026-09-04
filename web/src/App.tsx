import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import { ProtectedRoute } from './auth/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Leads from './pages/Leads';
import LeadDetail from './pages/LeadDetail';
import Assignments from './pages/Assignments';
import FollowUps from './pages/FollowUps';
import SiteVisits from './pages/SiteVisits';
import Sales from './pages/Sales';
import Properties from './pages/Properties';
import Customers from './pages/Customers';
import Tasks from './pages/Tasks';
import Reports from './pages/Reports';
import Users from './pages/Users';
import Audit from './pages/Audit';
import Notifications from './pages/Notifications';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="leads" element={<Leads />} />
        <Route path="leads/:id" element={<LeadDetail />} />
        <Route
          path="assignments"
          element={
            <ProtectedRoute roles={['ADMIN', 'SALES_MANAGER']}>
              <Assignments />
            </ProtectedRoute>
          }
        />
        <Route path="follow-ups" element={<FollowUps />} />
        <Route path="site-visits" element={<SiteVisits />} />
        <Route
          path="sales"
          element={
            <ProtectedRoute roles={['ADMIN', 'SALES_MANAGER', 'SALES_EXECUTIVE']}>
              <Sales />
            </ProtectedRoute>
          }
        />
        <Route path="properties" element={<Properties />} />
        <Route path="customers" element={<Customers />} />
        <Route path="tasks" element={<Tasks />} />
        <Route
          path="reports"
          element={
            <ProtectedRoute roles={['ADMIN', 'SALES_MANAGER']}>
              <Reports />
            </ProtectedRoute>
          }
        />
        <Route
          path="users"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <Users />
            </ProtectedRoute>
          }
        />
        <Route
          path="audit"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <Audit />
            </ProtectedRoute>
          }
        />
        <Route path="notifications" element={<Notifications />} />
      </Route>
      <Route path="*" element={<div className="p-10 text-center text-slate-400">404 — Page not found</div>} />
    </Routes>
  );
}
