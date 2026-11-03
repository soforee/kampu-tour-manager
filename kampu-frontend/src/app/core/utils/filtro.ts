
function permitido(tipo: string, c: string): boolean {
  if (tipo === 'entero' || tipo === 'dni') return /[0-9]/.test(c);
  if (tipo === 'decimal') return /[0-9.]/.test(c);
  if (tipo === 'telefono') return /[0-9+() -]/.test(c);
  if (tipo === 'alfanumerico') return /[A-Za-z0-9]/.test(c);
  if (tipo === 'nombre') return /[\p{L} .'-]/u.test(c);
  if (tipo === 'placa') return /[A-Za-z0-9-]/.test(c);
  if (tipo === 'codigo') return /[A-Za-z0-9 ._/#-]/.test(c);
  return true;
}

function limpiar(tipo: string, texto: string): string {
  let resultado = '';
  for (const c of texto) {
    if (permitido(tipo, c)) resultado += c;
  }
  return resultado;
}

export function filtrarEscritura(evento: Event, tipo: string) {
  const e = evento as InputEvent;
  const input = e.target as HTMLInputElement;
  if (e.inputType === 'insertFromDrop') { e.preventDefault(); return; }
  if (!e.data || e.isComposing || e.inputType === 'insertCompositionText' || e.inputType === 'insertFromPaste') return;
  if (limpiar(tipo, e.data) !== e.data) { e.preventDefault(); return; }
  if (tipo === 'decimal') {
    const actual = input.value;
    if (e.data.includes('.') && actual.includes('.')) { e.preventDefault(); return; }
    const partes = actual.split('.');
    if (partes.length === 2 && partes[1].length >= 2 && /[0-9]/.test(e.data) && cursorAlFinal(input)) e.preventDefault();
  }
}

export function filtrarPegado(evento: Event, tipo: string) {
  const e = evento as ClipboardEvent;
  const input = e.target as HTMLInputElement;
  const texto = e.clipboardData ? e.clipboardData.getData('text') : '';
  const limpio = limpiar(tipo, texto.trim());
  if (limpio === texto) return;
  e.preventDefault();
  if (input.type === 'number') {
    input.value = limpio;
  } else {
    const inicio = input.selectionStart !== null ? input.selectionStart : input.value.length;
    const fin = input.selectionEnd !== null ? input.selectionEnd : input.value.length;
    input.value = input.value.substring(0, inicio) + limpio + input.value.substring(fin);
  }
  input.dispatchEvent(new Event('input'));
}

export function sinRueda(evento: Event) {
  const input = evento.target as HTMLInputElement;
  if (document.activeElement === input) input.blur();
}

function cursorAlFinal(input: HTMLInputElement): boolean {
  try {
    return input.selectionStart === null || input.selectionStart === input.value.length;
  } catch {
    return true;
  }
}
