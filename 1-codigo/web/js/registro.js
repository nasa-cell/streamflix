(() => {
  const formulario = document.getElementById("formulario");
  const boton = document.getElementById("enviar");
  Campos.preparar(formulario);

  formulario.addEventListener("submit", async e => {
    e.preventDefault();
    if (!Campos.validarTodo(formulario)) return;
    const datos = Campos.datos(formulario);
    datos.edad = Number(datos.edad);
    boton.disabled = true;
    boton.classList.add("cargando");
    try {
      const r = await api("/cuenta/registro", datos);
      sessionStorage.setItem("sf_mensaje", r.mensaje);
      location.href = "perfil.html?primero=1";
    } catch (err) {
      mostrarError(err, formulario);
    } finally {
      boton.disabled = false;
      boton.classList.remove("cargando");
    }
  });
})();
