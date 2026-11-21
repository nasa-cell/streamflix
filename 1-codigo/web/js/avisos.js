// Avisos cortos (abajo a la derecha) y ventana de doble confirmación.
const Avisos = (() => {
  function caja() {
    let c = document.querySelector(".avisos");
    if (!c) {
      c = document.createElement("div");
      c.className = "avisos";
      c.setAttribute("role", "status");
      c.setAttribute("aria-live", "polite");
      document.body.appendChild(c);
    }
    return c;
  }

  const ICONOS = { ok: "✅", error: "⚠️", info: "💡" };

  function mostrar(texto, tipo = "ok", detalle = "") {
    const a = document.createElement("div");
    a.className = "aviso " + tipo;
    a.innerHTML = `<span class="icono">${ICONOS[tipo] || "💡"}</span><div><b></b><small></small></div>`;
    a.querySelector("b").textContent = texto;
    a.querySelector("small").textContent = detalle;
    caja().appendChild(a);
    if (tipo === "error") Sonido.error(); else if (tipo === "ok") Sonido.exito(); else Sonido.aviso();
    const tiempo = tipo === "error" ? 7000 : 3500;
    setTimeout(() => { a.classList.add("sale"); setTimeout(() => a.remove(), 320); }, tiempo);
    a.addEventListener("click", () => a.remove());
  }

  function ventana({ icono, paso, titulo, texto, si, no = "Cancelar", peligro = false }) {
    return new Promise(resolver => {
      const fondo = document.createElement("div");
      fondo.className = "fondo-modal";
      fondo.innerHTML = `<div class="modal ${peligro ? "peligro" : ""}" role="dialog" aria-modal="true">
          <div class="icono-grande"></div><div class="paso"></div><h2></h2><p></p>
          <div class="fila-botones"><button class="btn btn-2" data-r="no"></button>
          <button class="btn ${peligro ? "btn-peligro lleno" : "btn-1"}" data-r="si"></button></div></div>`;
      fondo.querySelector(".icono-grande").textContent = icono;
      fondo.querySelector(".paso").textContent = paso;
      fondo.querySelector("h2").textContent = titulo;
      fondo.querySelector("p").textContent = texto;
      fondo.querySelector('[data-r="no"]').textContent = no;
      fondo.querySelector('[data-r="si"]').textContent = si;
      document.body.appendChild(fondo);
      Sonido.aviso();
      const cerrar = r => { fondo.remove(); document.removeEventListener("keydown", tecla); resolver(r); };
      const tecla = e => { if (e.key === "Escape") cerrar(false); };
      document.addEventListener("keydown", tecla);
      fondo.addEventListener("click", e => {
        if (e.target === fondo) cerrar(false);
        const b = e.target.closest("[data-r]");
        if (b) cerrar(b.dataset.r === "si");
      });
      fondo.querySelector('[data-r="no"]').focus();
    });
  }

  /** Pide confirmar dos veces antes de borrar, eliminar o cerrar sesión. */
  async function confirmarDoble({ accion, que, consecuencia }) {
    const primero = await ventana({
      icono: "🤔", paso: "PASO 1 DE 2", titulo: `¿Quieres ${accion}${que ? " " + que : ""}?`,
      texto: consecuencia, si: "Sí, continuar",
    });
    if (!primero) { mostrar("Cancelado. No se cambió nada.", "info"); return false; }
    const segundo = await ventana({
      icono: "⚠️", paso: "PASO 2 DE 2 · ÚLTIMO AVISO", titulo: `¿Seguro que quieres ${accion}${que ? " " + que : ""}?`,
      texto: "Confírmalo una vez más para evitar errores.", si: `Sí, ${accion}`, no: "No, mejor no", peligro: true,
    });
    if (!segundo) mostrar("Cancelado. No se cambió nada.", "info");
    return segundo;
  }

  return { mostrar, confirmarDoble };
})();
