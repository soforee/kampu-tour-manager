// Códigos telefónicos y cantidad de dígitos del número nacional (sin el código) por país.
// Debe coincidir con Reglas.PAISES_TELEFONO del backend.
export interface PaisTelefono {
  iso: string;
  nombre: string;
  codigo: string;
  min: number;
  max: number;
  ejemplo: string;
}

export const PAISES: PaisTelefono[] = [
  { iso: 'PE', nombre: 'Perú', codigo: '51', min: 9, max: 9, ejemplo: '987654321' },
  { iso: 'AR', nombre: 'Argentina', codigo: '54', min: 10, max: 11, ejemplo: '91123456789' },
  { iso: 'BO', nombre: 'Bolivia', codigo: '591', min: 8, max: 8, ejemplo: '71234567' },
  { iso: 'BR', nombre: 'Brasil', codigo: '55', min: 10, max: 11, ejemplo: '11912345678' },
  { iso: 'CL', nombre: 'Chile', codigo: '56', min: 9, max: 9, ejemplo: '912345678' },
  { iso: 'CO', nombre: 'Colombia', codigo: '57', min: 10, max: 10, ejemplo: '3001234567' },
  { iso: 'CR', nombre: 'Costa Rica', codigo: '506', min: 8, max: 8, ejemplo: '83123456' },
  { iso: 'CU', nombre: 'Cuba', codigo: '53', min: 8, max: 8, ejemplo: '51234567' },
  { iso: 'EC', nombre: 'Ecuador', codigo: '593', min: 8, max: 9, ejemplo: '991234567' },
  { iso: 'GT', nombre: 'Guatemala', codigo: '502', min: 8, max: 8, ejemplo: '51234567' },
  { iso: 'MX', nombre: 'México', codigo: '52', min: 10, max: 10, ejemplo: '5512345678' },
  { iso: 'PA', nombre: 'Panamá', codigo: '507', min: 7, max: 8, ejemplo: '61234567' },
  { iso: 'PY', nombre: 'Paraguay', codigo: '595', min: 8, max: 9, ejemplo: '961456789' },
  { iso: 'UY', nombre: 'Uruguay', codigo: '598', min: 8, max: 8, ejemplo: '94231234' },
  { iso: 'VE', nombre: 'Venezuela', codigo: '58', min: 10, max: 10, ejemplo: '4121234567' },
  { iso: 'US', nombre: 'Estados Unidos', codigo: '1', min: 10, max: 10, ejemplo: '2015550123' },
  { iso: 'CA', nombre: 'Canadá', codigo: '1', min: 10, max: 10, ejemplo: '5062345678' },
  { iso: 'DE', nombre: 'Alemania', codigo: '49', min: 10, max: 11, ejemplo: '15123456789' },
  { iso: 'AU', nombre: 'Australia', codigo: '61', min: 9, max: 9, ejemplo: '412345678' },
  { iso: 'CN', nombre: 'China', codigo: '86', min: 11, max: 11, ejemplo: '13123456789' },
  { iso: 'KR', nombre: 'Corea del Sur', codigo: '82', min: 9, max: 10, ejemplo: '1020000000' },
  { iso: 'ES', nombre: 'España', codigo: '34', min: 9, max: 9, ejemplo: '612345678' },
  { iso: 'FR', nombre: 'Francia', codigo: '33', min: 9, max: 9, ejemplo: '612345678' },
  { iso: 'IN', nombre: 'India', codigo: '91', min: 10, max: 10, ejemplo: '8123456789' },
  { iso: 'IL', nombre: 'Israel', codigo: '972', min: 9, max: 9, ejemplo: '502345678' },
  { iso: 'IT', nombre: 'Italia', codigo: '39', min: 9, max: 10, ejemplo: '3123456789' },
  { iso: 'JP', nombre: 'Japón', codigo: '81', min: 10, max: 10, ejemplo: '9012345678' },
  { iso: 'NL', nombre: 'Países Bajos', codigo: '31', min: 9, max: 9, ejemplo: '612345678' },
  { iso: 'PT', nombre: 'Portugal', codigo: '351', min: 9, max: 9, ejemplo: '912345678' },
  { iso: 'GB', nombre: 'Reino Unido', codigo: '44', min: 10, max: 10, ejemplo: '7400123456' },
  { iso: 'RU', nombre: 'Rusia', codigo: '7', min: 10, max: 10, ejemplo: '9123456789' },
  { iso: 'CH', nombre: 'Suiza', codigo: '41', min: 9, max: 9, ejemplo: '781234567' },
];

export const PAIS_DEFECTO = PAISES[0];

// Bandera a partir del código ISO (🇵🇪, 🇨🇱…)
export function bandera(iso: string): string {
  return String.fromCodePoint(...iso.split('').map(c => 0x1F1E6 + c.charCodeAt(0) - 65));
}

// "+51 987654321" → país y número. Si no hay código, se asume Perú.
export function separarTelefono(valor: string | null | undefined): { pais: PaisTelefono; numero: string } {
  const t = (valor ?? '').trim();
  const m = /^\+(\d{1,3})\s*(.*)$/.exec(t);
  if (m) {
    const pais = PAISES.find(p => p.codigo === m[1]);
    if (pais) return { pais, numero: m[2].replace(/\D/g, '') };
  }
  return { pais: PAIS_DEFECTO, numero: t.replace(/\D/g, '') };
}

export function unirTelefono(pais: PaisTelefono, numero: string): string {
  const n = numero.replace(/\D/g, '');
  return n ? `+${pais.codigo} ${n}` : '';
}

// Mensaje de error o null (misma regla que el backend)
export function errorTelefonoPais(valor: string | null | undefined, obligatorio = true): string | null {
  const t = (valor ?? '').trim();
  if (!t) return obligatorio ? 'El teléfono es obligatorio.' : null;
  const m = /^\+(\d{1,3}) (\d+)$/.exec(t);
  if (!m) return 'Elige el código de país y escribe solo los dígitos del número.';
  const pais = PAISES.find(p => p.codigo === m[1]);
  if (!pais) return `El código +${m[1]} no está en la lista de países.`;
  const n = m[2].length;
  if (n < pais.min || n > pais.max) {
    const cantidad = pais.min === pais.max ? `${pais.min} dígitos` : `entre ${pais.min} y ${pais.max} dígitos`;
    return `Para ${pais.nombre} (+${pais.codigo}) el número debe tener ${cantidad}.`;
  }
  return null;
}
