import { TipoDocumentoCliente } from '../models';

export type ErrorCampo = string | null;

const NOMBRE_PERSONA = /^\p{L}[\p{L} .'-]*$/u;
const TEXTO = /^[\p{L}\p{N} .,;:'"()&/+#°ºª¡!¿?%-]*$/u;
const NACIONALIDAD = /^\p{L}[\p{L} ]*$/u;
const TELEFONO = /^\+?[0-9][0-9 ()-]*$/;
const EMAIL = /^[^@\s]+@[^@\s]+\.[A-Za-z]{2,}$/;
const PLACA = /^[A-Za-z0-9-]{3,10}$/;   // VEHICULO.placa VARCHAR2(10)
const RUC = /^(10|15|17|20)\d{9}$/;      // chk_prov_ruc
const CODIGO = /^[A-Za-z0-9 ._/#-]*$/;

const debe = (campo: string) => /^(Los|Las) /.test(campo) ? 'deben' : 'debe';
const ingresa = (campo: string) => `Ingresa ${campo.charAt(0).toLowerCase() + campo.slice(1)} (solo números).`;

const vacio = (v: unknown) => v === null || v === undefined || String(v).trim() === '';

export const V = {
  requerido(v: unknown, campo: string): ErrorCampo {
    return vacio(v) ? `${campo} es obligatorio.` : null;
  },

  nombrePersona(v: string, campo = 'El nombre', max = 150): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return `${campo} es obligatorio.`;
    if (t.length > max) return `${campo} admite máximo ${max} caracteres.`;
    return NOMBRE_PERSONA.test(t) ? null : `${campo} solo admite letras, espacios y los signos . ' -`;
  },

  texto(v: string, campo: string, max: number, obligatorio = false): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return obligatorio ? `${campo} es obligatorio.` : null;
    if (t.length > max) return `${campo} admite máximo ${max} caracteres.`;
    return TEXTO.test(t) ? null : `${campo} contiene caracteres no permitidos.`;
  },

  nacionalidad(v: string): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return null;
    if (t.length > 50) return 'La nacionalidad admite máximo 50 caracteres.';
    return NACIONALIDAD.test(t) ? null : 'La nacionalidad solo admite letras y espacios.';
  },

  documento(tipo: TipoDocumentoCliente, v: string): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return 'El número de documento es obligatorio.';
    if (tipo === 'RUC') return RUC.test(t) ? null : 'El RUC tiene 11 dígitos y empieza con 10, 15, 17 o 20.';
    if (tipo === 'DNI') return /^\d{8}$/.test(t) ? null : 'El DNI debe tener exactamente 8 dígitos.';
    if (tipo === 'CE') return /^[A-Za-z0-9]{8,12}$/.test(t) ? null : 'El carné de extranjería tiene de 8 a 12 letras o números.';
    return /^[A-Za-z0-9]{6,12}$/.test(t) ? null : 'El pasaporte tiene de 6 a 12 letras o números.';
  },

  email(v: string): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return null;
    if (t.length > 100) return 'El correo admite máximo 100 caracteres.';
    return EMAIL.test(t) ? null : 'Escribe un correo válido (ejemplo: nombre@dominio.com).';
  },

  telefono(v: string): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return null;
    if (t.length > 20) return 'El teléfono admite máximo 20 caracteres.';
    const digitos = t.replace(/\D/g, '').length;
    return TELEFONO.test(t) && digitos >= 6 && digitos <= 15 ? null : 'El teléfono debe tener entre 6 y 15 dígitos (puede llevar + ( ) -).';
  },

  placa(v: string): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return 'La placa es obligatoria.';
    return PLACA.test(t) ? null : 'La placa solo admite letras, números y guion (3 a 10 caracteres).';
  },

  ruc(v: string): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return 'El RUC es obligatorio.';
    return RUC.test(t) ? null : 'El RUC tiene 11 dígitos y empieza con 10, 15, 17 o 20.';
  },

  codigo(v: string): ErrorCampo {
    const t = (v ?? '').trim();
    if (!t) return null;
    if (t.length > 100) return 'El código admite máximo 100 caracteres.';
    return CODIGO.test(t) ? null : 'El código solo admite letras, números y . _ / # -';
  },

  entero(v: number | null, campo: string, min: number, max: number): ErrorCampo {
    if (v === null || v === undefined || (typeof v === 'number' && isNaN(v))) return ingresa(campo);
    if (!Number.isInteger(v)) return `${campo} ${debe(campo)} ser un número entero.`;
    return v < min || v > max ? `${campo} ${debe(campo)} estar entre ${min} y ${max}.` : null;
  },

  importe(v: number | null, campo: string): ErrorCampo {
    if (v === null || v === undefined || (typeof v === 'number' && isNaN(v))) return ingresa(campo);
    if (v <= 0) return `${campo} ${debe(campo)} ser mayor a 0.`;
    if (v > 99999999.99) return `${campo} no puede superar 99,999,999.99.`;
    return Math.round(v * 100) / 100 === v ? null : `${campo} admite como máximo 2 decimales.`;
  },

  fecha(v: string, campo: string, obligatorio = true): ErrorCampo {
    if (!v) return obligatorio ? `${campo} es obligatoria.` : null;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(v) || isNaN(new Date(v + 'T00:00:00').getTime())) return `${campo} no es una fecha válida.`;
    return null;
  }
};

// Solo los dígitos de un teléfono: así se compara si está repetido (igual que el backend)
export function digitos(telefono?: string | null): string {
  return (telefono ?? '').replace(/\D/g, '');
}

export function telefonoRepetido<T>(telefono: string, lista: T[], obtener: (x: T) => string | null | undefined,
                                    esMismo: (x: T) => boolean): boolean {
  const d = digitos(telefono);
  return !!d && lista.some(x => !esMismo(x) && digitos(obtener(x)) === d);
}

export interface Errores {
  [campo: string]: string | null | undefined;
}

export class FormValidacion {
  private tocados: { [campo: string]: boolean } = {};
  private intentado = false;
  private reglas: () => Errores;

  constructor(reglas: () => Errores) {
    this.reglas = reglas;
  }

  tocar(campo: string) {
    this.tocados[campo] = true;
  }

  error(campo: string): ErrorCampo {
    if (!this.intentado && !this.tocados[campo]) return null;
    const errores = this.reglas();
    const mensaje = errores[campo];
    return mensaje ? mensaje : null;
  }

  valido(): boolean {
    this.intentado = true;
    const errores = this.reglas();
    for (const campo in errores) {
      if (errores[campo]) return false;
    }
    return true;
  }

  // Valida solo algunos campos (un paso de un asistente): los marca como tocados para mostrar sus errores
  validos(campos: string[]): boolean {
    const errores = this.reglas();
    campos.forEach(c => this.tocar(c));
    return campos.every(c => !errores[c]);
  }

  reiniciar() {
    this.tocados = {};
    this.intentado = false;
  }
}
