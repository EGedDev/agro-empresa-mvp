export type ProductoComercial = {
  id: string;
  nombre: string;
  categoria: string;
  linea: string;
  descripcion: string;
  precioVenta?: number | string | null;
  beneficios: string[];
  uso: string;
  imagen: string;
  destacado?: boolean;
};

export type BannerComercial = {
  titulo: string;
  subtitulo: string;
  imagen: string;
  producto: string;
};

const productoBase = "/comercial/productos";
const anuncioBase = "/comercial/anuncios";
const portadaBase = "/comercial/portadas";

export const categoriasComerciales = [
  "Todos",
  "Nutricion",
  "Bioestimulantes",
  "Suelo y raices",
  "Proteccion",
  "Calidad de fruto"
];

export const productosComerciales: ProductoComercial[] = [
  {
    id: "potasio",
    nombre: "Potasio",
    categoria: "Nutricion",
    linea: "Fertilizante cristalino soluble",
    descripcion: "Formula enfocada en llenado, firmeza y calidad final del cultivo.",
    beneficios: ["Mejora calibre y maduracion", "Apoya transporte de azucares", "Ideal para etapas productivas"],
    uso: "Frutales, hortalizas y cultivos en llenado.",
    imagen: `${productoBase}/potasio.jpeg`,
    destacado: true
  },
  {
    id: "fosforo",
    nombre: "Fosforo",
    categoria: "Nutricion",
    linea: "Fertilizante cristalino soluble",
    descripcion: "Nutricion dirigida a energia, floracion y desarrollo radicular temprano.",
    beneficios: ["Activa crecimiento inicial", "Soporta floracion", "Favorece raices funcionales"],
    uso: "Arranque, trasplante y prefloracion.",
    imagen: `${productoBase}/fosforo.jpeg`
  },
  {
    id: "super-silmag",
    nombre: "Super Silmag",
    categoria: "Nutricion",
    linea: "Silicio + magnesio",
    descripcion: "Complemento para plantas mas firmes, verdes y tolerantes al estres.",
    beneficios: ["Refuerza tejidos", "Mejora actividad fotosintetica", "Apoya tolerancia ambiental"],
    uso: "Programas de nutricion foliar y fertirriego.",
    imagen: `${productoBase}/super-silmag.jpeg`
  },
  {
    id: "nitrogeno",
    nombre: "Nitrogeno",
    categoria: "Nutricion",
    linea: "Fertilizante cristalino soluble",
    descripcion: "Fuente de nitrogeno para crecimiento vegetativo uniforme y vigoroso.",
    beneficios: ["Impulsa brotes", "Acompana recuperacion del cultivo", "Aporta vigor controlado"],
    uso: "Crecimiento, recuperacion y mantenimiento.",
    imagen: `${productoBase}/nitrogeno.jpeg`
  },
  {
    id: "citcomax-plus",
    nombre: "Citcomax Plus",
    categoria: "Bioestimulantes",
    linea: "Bioestimulante de calidad",
    descripcion: "Especialista en mejorar propiedades organolepticas y vida de anaquel.",
    beneficios: ["Optimiza color y sabor", "Favorece consistencia", "Apoya poscosecha"],
    uso: "Frutales, berries, citricos y hortalizas de alto valor.",
    imagen: `${productoBase}/citcomax-plus.jpeg`,
    destacado: true
  },
  {
    id: "combat",
    nombre: "Combat",
    categoria: "Proteccion",
    linea: "Promotor de respuesta inmune",
    descripcion: "Sulfato de cobre pentahidratado para activar defensas del cultivo.",
    beneficios: ["Ayuda a prevenir enfermedades", "Fortalece respuesta natural", "Compatible con manejo preventivo"],
    uso: "Programas de sanidad agricola.",
    imagen: `${productoBase}/combat.jpeg`
  },
  {
    id: "conan",
    nombre: "Conan",
    categoria: "Bioestimulantes",
    linea: "Citocininas + auxinas + aminoacidos",
    descripcion: "Estimula division celular, crecimiento de brotes y desarrollo vegetativo.",
    beneficios: ["Uniformiza brotacion", "Favorece desarrollo foliar", "Acompana recuperacion por estres"],
    uso: "Brotamiento, crecimiento y recuperacion.",
    imagen: `${productoBase}/conan.jpeg`
  },
  {
    id: "bio-mite",
    nombre: "Bio-Mite",
    categoria: "Proteccion",
    linea: "Manejo preventivo",
    descripcion: "Apoyo especializado para programas de proteccion y equilibrio del cultivo.",
    beneficios: ["Refuerza manejo integrado", "Apto para rotaciones", "Enfoque preventivo"],
    uso: "Hortalizas, frutales y cultivos intensivos.",
    imagen: `${productoBase}/bio-mite.jpeg`
  },
  {
    id: "biofit",
    nombre: "Biofit",
    categoria: "Bioestimulantes",
    linea: "Inductor de brote",
    descripcion: "Formula para activar crecimiento y mejorar uniformidad del cultivo.",
    beneficios: ["Promueve brotes", "Mejora vigor", "Apoya recuperacion vegetativa"],
    uso: "Etapas de crecimiento y rebrote.",
    imagen: `${productoBase}/biofit.jpeg`
  },
  {
    id: "auxiven-max",
    nombre: "Auxiven Max",
    categoria: "Bioestimulantes",
    linea: "Maximiza el brotamiento",
    descripcion: "Desarrollado para impulsar brotes activos y crecimiento ordenado.",
    beneficios: ["Activa yemas", "Mejora uniformidad", "Potencia crecimiento inicial"],
    uso: "Vid, frutales, hortalizas y cultivos con brotacion exigente.",
    imagen: `${productoBase}/auxiven-max.jpeg`
  },
  {
    id: "bioplus-max",
    nombre: "Bioplus Max",
    categoria: "Bioestimulantes",
    linea: "Aminoacidos y energia",
    descripcion: "Apoyo nutricional para etapas de demanda y recuperacion.",
    beneficios: ["Acompana estres", "Mejora vigor", "Favorece metabolismo"],
    uso: "Postrasplante, floracion y recuperacion.",
    imagen: `${productoBase}/bioplus-max.jpeg`
  },
  {
    id: "fotomax",
    nombre: "Fotomax",
    categoria: "Bioestimulantes",
    linea: "Inductor de fotosintesis",
    descripcion: "Incrementa asimilacion de carbono y eficiencia del area foliar.",
    beneficios: ["Mejora actividad fotosintetica", "Apoya energia de planta", "Favorece rendimiento"],
    uso: "Maiz, hortalizas, frutales y cultivos con alta demanda.",
    imagen: `${productoBase}/fotomax.jpeg`
  },
  {
    id: "fruit-max",
    nombre: "Fruit Max",
    categoria: "Calidad de fruto",
    linea: "Calidad y maduracion",
    descripcion: "Especialista para obtener frutos mas uniformes, firmes y comerciales.",
    beneficios: ["Mejora acabado", "Apoya firmeza", "Favorece calibre"],
    uso: "Etapas de crecimiento y maduracion de fruto.",
    imagen: `${productoBase}/fruit-max.jpeg`,
    destacado: true
  },
  {
    id: "humiven-max",
    nombre: "Humiven Max",
    categoria: "Suelo y raices",
    linea: "Activador de suelos",
    descripcion: "Mejora condiciones del suelo y disponibilidad de nutrientes.",
    beneficios: ["Aumenta actividad del suelo", "Favorece absorcion", "Mejora estructura"],
    uso: "Suelos cansados, fertirriego y programas de recuperacion.",
    imagen: `${productoBase}/humiven-max.jpeg`
  },
  {
    id: "karacit",
    nombre: "Karacit",
    categoria: "Nutricion",
    linea: "Nutricion especializada",
    descripcion: "Solucion de aporte dirigido para cultivos de alta exigencia.",
    beneficios: ["Apoya equilibrio nutricional", "Mejora respuesta de planta", "Acompana etapas criticas"],
    uso: "Programas foliares y fertirriego.",
    imagen: `${productoBase}/karacit.jpeg`
  },
  {
    id: "nemaplus-max",
    nombre: "Nemaplus Max",
    categoria: "Proteccion",
    linea: "Raiz protegida",
    descripcion: "Apoyo para sistemas radiculares mas sanos y funcionales.",
    beneficios: ["Protege zona radicular", "Favorece raices activas", "Acompana manejo integrado"],
    uso: "Suelos con presion y cultivos intensivos.",
    imagen: `${productoBase}/nemaplus-max.jpeg`
  },
  {
    id: "saltrex-max",
    nombre: "Saltrex Max",
    categoria: "Suelo y raices",
    linea: "Dispersante de sales",
    descripcion: "Ayuda a mejorar estructura del suelo y reducir efecto de sales.",
    beneficios: ["Promueve lavado de sales", "Mejora infiltracion", "Libera calcio para intercambio"],
    uso: "Suelos salinos, fertirriego y recuperacion de campos.",
    imagen: `${productoBase}/saltrex-max.jpeg`
  },
  {
    id: "rain-roots",
    nombre: "Rain Roots",
    categoria: "Suelo y raices",
    linea: "Incrementa el enraizamiento",
    descripcion: "Formula para mayor produccion de raices y mejor anclaje del cultivo.",
    beneficios: ["Incrementa raices activas", "Mejora absorcion", "Apoya trasplante"],
    uso: "Plantines, trasplante y recuperacion radicular.",
    imagen: `${productoBase}/rain-roots.jpeg`,
    destacado: true
  },
  {
    id: "phospati",
    nombre: "Phospati",
    categoria: "Nutricion",
    linea: "Energia y floracion",
    descripcion: "Aporta fosforo y potasio para vigor, floracion y calidad.",
    beneficios: ["Fortalece energia del cultivo", "Acompana floracion", "Mejora produccion"],
    uso: "Prefloracion, cuajado y fases productivas.",
    imagen: `${productoBase}/phospati.jpeg`
  },
  {
    id: "setplus",
    nombre: "Setplus",
    categoria: "Nutricion",
    linea: "Microelementos mayores",
    descripcion: "Complejo nutricional para corregir deficiencias y sostener rendimiento.",
    beneficios: ["Aporta micronutrientes", "Evita desbalances", "Mejora desarrollo general"],
    uso: "Programas de mantenimiento nutricional.",
    imagen: `${productoBase}/setplus.jpeg`
  },
  {
    id: "sugar-max",
    nombre: "Sugar Max",
    categoria: "Calidad de fruto",
    linea: "Optimizador del fruto",
    descripcion: "Dirigido a mejorar calidad, dulzor y vida de anaquel.",
    beneficios: ["Mejora grados brix", "Optimiza acabado", "Prolonga vida del fruto"],
    uso: "Berries, citricos, uva, palto y hortalizas de fruto.",
    imagen: `${productoBase}/sugar-max.jpeg`
  },
  {
    id: "twister",
    nombre: "Twister",
    categoria: "Bioestimulantes",
    linea: "Energia y potencia",
    descripcion: "Incrementa vigor, brotacion y disponibilidad de nutrientes.",
    beneficios: ["Aporta energia metabolica", "Mejora brotes", "Acompana recuperacion"],
    uso: "Etapas de activacion y crecimiento.",
    imagen: `${productoBase}/twister.jpeg`
  },
  {
    id: "full-power-calcio",
    nombre: "Full Power Calcio",
    categoria: "Nutricion",
    linea: "Calcio asimilable",
    descripcion: "Fortalece estructura celular y calidad comercial del fruto.",
    beneficios: ["Reduce problemas de firmeza", "Mejora resistencia de tejidos", "Apoya poscosecha"],
    uso: "Frutales, hortalizas y cultivos sensibles a calcio.",
    imagen: `${productoBase}/full-power-calcio.jpeg`
  },
  {
    id: "ph-super",
    nombre: "pH Super",
    categoria: "Nutricion",
    linea: "Corrector de pH",
    descripcion: "Acondicionador de agua para aplicaciones mas eficientes.",
    beneficios: ["Corrige pH del agua", "Mejora mezcla", "Optimiza aplicaciones"],
    uso: "Preparacion de caldos y aplicaciones foliares.",
    imagen: `${productoBase}/ph-super.jpeg`
  },
  {
    id: "zoik-max",
    nombre: "Zoik Max",
    categoria: "Nutricion",
    linea: "Traslado de nutrientes",
    descripcion: "Promueve movilidad y mejor aprovechamiento nutricional.",
    beneficios: ["Mejora transporte interno", "Favorece llenado", "Potencia respuesta productiva"],
    uso: "Etapas de demanda alta y llenado.",
    imagen: `${productoBase}/zoik-max.jpeg`
  },
  {
    id: "pretor",
    nombre: "Pretor",
    categoria: "Proteccion",
    linea: "Proteccion de cultivo",
    descripcion: "Apoyo preventivo para mantener cultivos sanos y productivos.",
    beneficios: ["Acompana manejo integrado", "Protege vigor del cultivo", "Reduce riesgo operativo"],
    uso: "Programas de sanidad y mantenimiento.",
    imagen: `${productoBase}/pretor.jpeg`
  },
  {
    id: "alga-plus",
    nombre: "Alga Plus",
    categoria: "Bioestimulantes",
    linea: "Extracto de algas",
    descripcion: "Bioestimulante para vigor, tolerancia y recuperacion fisiologica.",
    beneficios: ["Aumenta vigor", "Acompana estres", "Favorece desarrollo equilibrado"],
    uso: "Postestres, crecimiento y etapas productivas.",
    imagen: `${productoBase}/alga-plus.jpeg`
  }
];

export const bannersComerciales: BannerComercial[] = [
  {
    titulo: "Cultivos libres de enfermedades",
    subtitulo: "Linea de proteccion y nutricion para sostener rendimiento en campo.",
    imagen: `${portadaBase}/cultivos-libres-enfermedades.jpeg`,
    producto: "Combat"
  },
  {
    titulo: "Impulsa el enraizamiento",
    subtitulo: "Raices activas, absorcion estable y plantas con mejor arranque.",
    imagen: `${portadaBase}/enraizamiento-cultivos.jpeg`,
    producto: "Rain Roots"
  },
  {
    titulo: "Antiestres + peptidos",
    subtitulo: "Recuperacion, vigor y calidad para etapas criticas.",
    imagen: `${portadaBase}/linea-antistress-peptidos.jpeg`,
    producto: "Citcomax Plus"
  }
];

export const piezasPromocionales = [
  `${anuncioBase}/phospati-energia.jpeg`,
  `${anuncioBase}/combat-respuesta-inmune.jpeg`,
  `${anuncioBase}/citcomax-calidad-fruto.jpeg`,
  `${anuncioBase}/rain-roots-enraizamiento.jpeg`,
  `${anuncioBase}/sugar-calidad-fruto.jpeg`,
  `${anuncioBase}/fotomax-fotosintesis.jpeg`
];
