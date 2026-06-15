import type { Metadata, Viewport } from "next";
import "./globals.css";
import "../src/styles.css";
import "../src/commercial.css";

export const metadata: Metadata = {
  title: "ITAVEN ERP",
  description: "Web comercial y ERP interno para operaciones agricolas ITAVEN."
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
