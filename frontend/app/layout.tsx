import type { Metadata, Viewport } from "next";
import "./globals.css";
import "../src/styles.css";
import "../src/commercial.css";

export const metadata: Metadata = {
  title: 'ITAVENsac | Tecnología Agrícola y Fertilizantes en Perú',
  description: 'En ITAVEN SAC ofrecemos bioestimulantes, fertilizantes y soluciones de alta tecnología conectadas a nuestro ERP para optimizar el agro peruano.',
  keywords: ['ITAVEN', 'ITAVENsac', 'ITAVEN SAC', 'fertilizantes Perú', 'tecnología agrícola'],
  robots: {
    index: true,
    follow: true,
  },
  alternates: {
    canonical: 'https://itavensac.com', // Cambia esto si usas el .com o .com.pe definitivo
  }
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
