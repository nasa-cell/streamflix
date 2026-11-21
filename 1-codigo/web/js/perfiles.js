(() => {
  const zona = document.getElementById("zona");
  const lista = document.getElementById("perfiles");
  const botonAdministrar = document.getElementById("administrar");
  let administrando = parametro("administrar") === "1";
  let datos = null;

  async function cargar() {
    try {
      datos = await api("/perfiles/");
      pintar();
    } catch (e) {
      mostrarError(e);
      lista.innerHTML = `<p class="vacio"><b>😕</b>${escapar(e.message)}<br><small>${escapar(e.detalle || "")}</small></p>`;
    }
  }

  function pintar() {
    zona.classList.toggle("administrando", administrando);
    document.getElementById("titulo").textContent = administrando ? "Administrar perfiles" : "¿Quién está viendo?";
    botonAdministrar.innerHTML = administrando ? "✔ Listo" : '✏️ Administrar<span class="solo-ancho"> perfiles</span>';
    botonAdministrar.classList.toggle("activo", administrando);
    const p = datos.perfiles;
    lista.innerHTML = p.map(x => `
      <div class="perfil" role="button" tabindex="0" data-id="${x.id}" aria-label="Entrar como ${escapar(x.nombre)}">
        <div class="avatar" style="background:${x.color}">${x.avatar}</div>
        <p>${escapar(x.nombre)}</p>${x.infantil ? '<span class="infantil">Infantil</span>' : ""}
        <div class="acciones">
          <button class="mini-accion editar" data-editar="${x.id}" title="Editar ${escapar(x.nombre)}">✏️</button>
          <button class="mini-accion borrar" data-borrar="${x.id}" title="Borrar ${escapar(x.nombre)}">🗑</button>
        </div>
      </div>`).join("")
      + (p.length < datos.maximo ? `<a class="perfil nuevo" href="perfil.html"><div class="avatar">＋</div><p>Crear perfil</p></a>` : "");
    document.getElementById("nota").textContent = administrando
      ? "Toca ✏️ para cambiar un perfil o 🗑 para borrarlo (te pediremos confirmar dos veces)."
      : p.length >= datos.maximo
        ? `⚠️ Llegaste al máximo de ${datos.maximo} perfiles por cuenta. Para crear otro, toca «Administrar perfiles» y borra uno.`
        : `Tienes ${p.length} de ${datos.maximo} perfiles. Cada uno tiene sus propios gustos y recomendaciones.`;
  }

  async function elegir(id) {
    try {
      const r = await api("/perfiles/elegir", { id });
      sessionStorage.setItem("sf_mensaje", `Hola, ${r.perfil.nombre} ${r.perfil.avatar}`);
      location.href = "inicio.html";
    } catch (e) { mostrarError(e); }
  }

  async function borrar(id) {
    const p = datos.perfiles.find(x => x.id === id);
    const ok = await Avisos.confirmarDoble({
      accion: "borrar", que: `el perfil «${p.nombre}»`,
      consecuencia: "Se borrarán sus estrellas, comentarios, lista y lo que vio. Esto no se puede deshacer.",
    });
    if (!ok) return;
    try {
      const r = await api("/perfiles/borrar", { id });
      Avisos.mostrar(r.mensaje, "ok");
      await cargar();
    } catch (e) { mostrarError(e); }
  }

  lista.addEventListener("click", e => {
    const ed = e.target.closest("[data-editar]");
    const bo = e.target.closest("[data-borrar]");
    if (ed) { e.stopPropagation(); location.href = "perfil.html?id=" + ed.dataset.editar; return; }
    if (bo) { e.stopPropagation(); borrar(Number(bo.dataset.borrar)); return; }
    const perfil = e.target.closest(".perfil[data-id]");
    if (perfil) administrando ? (location.href = "perfil.html?id=" + perfil.dataset.id) : elegir(Number(perfil.dataset.id));
  });
  lista.addEventListener("keydown", e => {
    const perfil = e.target.closest(".perfil[data-id]");
    if (perfil && e.key === "Enter") elegir(Number(perfil.dataset.id));
  });
  botonAdministrar.addEventListener("click", () => { administrando = !administrando; pintar(); });
  document.getElementById("salir").addEventListener("click", Menu.cerrarSesion);
  cargar();
})();
