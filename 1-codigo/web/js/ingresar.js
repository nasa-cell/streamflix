(() => {
  const formulario = document.getElementById("formulario");
  const boton = document.getElementById("enviar");
  Campos.preparar(formulario);
  const puntos = document.getElementById("puntos");
  const chip = document.getElementById("chip-espera");
  let reloj = null;

  function pintarIntentos(quedan) {
    [...puntos.children].forEach((p, i) => p.classList.toggle("gastado", i >= quedan));
    puntos.setAttribute("aria-label", `Te quedan ${quedan} intentos`);
  }
  const mmss = s => Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0");

  function bloquear(segundos) {
    let fin = Date.now() + segundos * 1000;
    pintarIntentos(0);
    boton.disabled = true;
    chip.hidden = false;
    clearInterval(reloj);
    const tic = () => {
      const s = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
      chip.textContent = "🔒 " + mmss(s);
      if (s <= 0) {
        clearInterval(reloj);
        chip.hidden = true;
        boton.disabled = false;
        pintarIntentos(3);
      }
    };
    tic();
    reloj = setInterval(tic, 1000);
  }

  formulario.addEventListener("submit", async e => {
    e.preventDefault();
    if (!Campos.validarTodo(formulario)) return;
    boton.disabled = true;
    boton.classList.add("cargando");
    try {
      const r = await api("/cuenta/ingresar", Campos.datos(formulario));
      sessionStorage.setItem("sf_mensaje", `¡Hola, ${r.usuario.nombreCompleto.split(" ")[0]}!`);
      location.href = "perfiles.html";
    } catch (err) {
      mostrarError(err, formulario);
      const d = err.datos || {};
      if (typeof d.intentosQuedan === "number") pintarIntentos(d.intentosQuedan);
      if (d.bloqueado) { boton.classList.remove("cargando"); bloquear(d.segundos); return; }
    } finally {
      if (chip.hidden) boton.disabled = false;
      boton.classList.remove("cargando");
    }
  });
})();
