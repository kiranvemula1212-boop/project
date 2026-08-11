import { BrowserRouter, Route, Routes } from "react-router-dom";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ReportDetailPage } from "@/pages/ReportDetailPage";
import { ReportsLandingPage } from "@/pages/ReportsLandingPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ReportsLandingPage />} />
        <Route path="/reports/:reportId" element={<ReportDetailPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
