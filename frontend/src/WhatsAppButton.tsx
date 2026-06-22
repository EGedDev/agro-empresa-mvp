"use client";

import { FaWhatsapp } from "react-icons/fa";

export default function WhatsAppButton() {
return ( <a
   href="https://wa.me/51983971947?text=Hola%20Agro%20Empresa%2C%20estoy%20interesado%20en%20sus%20productos.%20¿Podrían%20proporcionarme%20más%20información%3F"
   target="_blank"
   rel="noopener noreferrer"
   aria-label="Contactar por WhatsApp"
   className="whatsapp-button"
 > <FaWhatsapp size={30} /> </a>
);
}
