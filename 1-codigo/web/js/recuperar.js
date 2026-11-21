// Recuperar la contraseña en tres pasos: Gmail, código temporal y nueva contraseña.
(() => {
  const formularios = [1, 2, 3].map(n => document.getElementById("paso" + n));
  const otro = document.getElementById("otro");
  const vence = document.getElementById("vence");
  let login = "", permiso = "", relojOtro = null, relojVence = null;
  formularios.forEach(f => Campos.preparar(f));

  const mmss = s => Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0");

  function paso(n) {
    formularios.forEach((f, i) => { f.hidden = i + 1 !== n; });
    document.querySelectorAll(".paso-ficha").forEach(f => {
      const k = +f.dataset.p;
      f.classList.toggle("actual", k === n);
      f.classList.toggle("hecho", k < n);
    });
    const primero = formularios[n - 1].querySelector("input");
    if (primero) primero.focus();
  }

  function cuentaOtro(segundos) {
    let fin = Date.now() + segundos * 1000;
    otro.disabled = true;
    clearInterval(relojOtro);
    const tic = () => {
      const s = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
      otro.textContent = s > 0 ? `Pedir otro código en ${s} s` : "Pedir otro código";
      if (s <= 0) { clearInterval(relojOtro); otro.disabled = false; }
    };
    tic();
    relojOtro = setInterval(tic, 1000);
  }

  function cuentaVence(minutos) {
    const fin = Date.now() + minutos * 60000;
    clearInterval(relojVence);
    const tic = () => {
      const s = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
      vence.textContent = s > 0 ? `Vence en ${mmss(s)}. Tienes 3 intentos.` : "El código venció. Pide otro.";
      if (s <= 0) clearInterval(relojVence);
    };
    tic();
    relojVence = setInterval(tic, 1000);
  }

  async function enviar(formulario, tarea) {
    if (!Campos.validarTodo(formulario)) return;
    const boton = formulario.querySelector('button[type="submit"]');
    boton.disabled = true;
    boton.classList.add("cargando");
    try {
      await tarea();
    } catch (err) {
      mostrarError(err, formulario);
      const d = err.datos || {};
      if (d.anulado) { document.getElementById("codigo").value = ""; paso(1); }
      if (formulario.id === "paso1" && d.segundos) cuentaOtro(d.segundos);
    } finally {
      boton.disabled = false;
      boton.classList.remove("cargando");
    }
  }

  async function pedirCodigo() {
    const r = await api("/cuenta/recuperar/pedir", { login });
    const e = r.envio;
    document.getElementById("aviso-envio").innerHTML = e.enviado
      ? `Te enviamos el código a <b>${e.correo.replace(/[<>&"]/g, "")}</b>. Revisa también «Spam».`
      : `No se pudo enviar el correo ahora. Como es una prueba local, el código aparece en la ventana negra del servidor (cuenta ${e.correo.replace(/[<>&"]/g, "")}).`;
    cuentaOtro(e.segundosOtro);
    cuentaVence(e.minutos);
    paso(2);
    Avisos.mostrar(e.enviado ? "Código enviado a tu Gmail." : "Código listo en la ventana del servidor.", "ok");
  }

  formularios[0].addEventListener("submit", e => {
    e.preventDefault();
    enviar(formularios[0], () => { login = document.getElementById("login").value.trim(); return pedirCodigo(); });
  });

  otro.addEventListener("click", async () => {
    otro.disabled = true;
    try { await pedirCodigo(); }
    catch (err) { mostrarError(err); if (err.datos && err.datos.segundos) cuentaOtro(err.datos.segundos); else otro.disabled = false; }
  });

  formularios[1].addEventListener("submit", e => {
    e.preventDefault();
    enviar(formularios[1], async () => {
      const r = await api("/cuenta/recuperar/comprobar", { login, codigo: document.getElementById("codigo").value });
      permiso = r.permiso;
      clearInterval(relojVence);
      paso(3);
      Avisos.mostrar("Código correcto. Crea tu nueva contraseña.", "ok");
    });
  });

  formularios[2].addEventListener("submit", e => {
    e.preventDefault();
    enviar(formularios[2], async () => {
      const r = await api("/cuenta/recuperar/cambiar", {
        permiso, clave: document.getElementById("clave").value, clave2: document.getElementById("clave2").value
      });
      sessionStorage.setItem("sf_mensaje", r.mensaje);
      location.href = "perfiles.html";
    });
  });

  paso(1);
})();
