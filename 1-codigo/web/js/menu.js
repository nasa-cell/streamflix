// Menú superior de las páginas internas: enlaces, sonido y perfil.
const Menu = (() => {
  const ENLACES = [
    ["inicio.html", "Inicio"], ["buscar.html", "Buscar"], ["inicio.html#mi-lista", "Mi lista"],
    ["gustos.html", "Mis gustos"], ["ia.html", "Cómo funciona la IA"],
  ];

  function logo() {
    return '<a class="logo-mini" href="inicio.html" aria-label="StreamFlix, ir al inicio">'
      + "StreamFlix".split("").map(l => `<b>${l}</b>`).join("") + "</a>";
  }

  async function pintar() {
    const header = document.getElementById("menu");
    const pagina = location.pathname.split("/").pop() || "inicio.html";
    header.className = "menu";
    header.innerHTML = `<div class="contenedor">
        <button class="boton-icono hamburguesa" aria-label="Abrir menú">☰</button>
        ${logo()}
        <nav class="enlaces">${ENLACES.map(([h, t]) => `<a href="${h}" class="${h === pagina ? "actual" : ""}">${t}</a>`).join("")}</nav>
        <div class="menu-derecha">
          <button class="boton-icono" id="boton-sonido" title="Sonido"></button>
          <div class="perfil-menu">
            <button class="perfil-boton" id="boton-perfil" aria-haspopup="true"><span class="avatar-mini">…</span><span></span> ▾</button>
            <div class="desplegable" id="desplegable">
              <a href="gustos.html">💡 Mis gustos</a>
              <a href="perfiles.html">🔄 Cambiar de perfil</a>
              <a href="perfiles.html?administrar=1">✏️ Administrar perfiles</a>
              <button class="peligro" id="cerrar-sesion">🚪 Cerrar sesión</button>
            </div>
          </div>
        </div></div>`;

    const sonido = document.getElementById("boton-sonido");
    const pintarSonido = () => { sonido.textContent = Sonido.activo ? "🔊" : "🔇"; sonido.title = Sonido.activo ? "Sonido encendido (clic para apagar)" : "Sonido apagado (clic para encender)"; };
    pintarSonido();
    sonido.addEventListener("click", () => { Sonido.cambiar(); pintarSonido(); });

    header.querySelector(".hamburguesa").addEventListener("click", () => header.querySelector(".enlaces").classList.toggle("abierto"));
    const desplegable = document.getElementById("desplegable");
    document.getElementById("boton-perfil").addEventListener("click", e => { e.stopPropagation(); desplegable.classList.toggle("abierto"); });
    document.addEventListener("click", () => desplegable.classList.remove("abierto"));
    document.getElementById("cerrar-sesion").addEventListener("click", cerrarSesion);

    try {
      const { perfil } = await api("/perfiles/actual");
      const boton = document.getElementById("boton-perfil");
      boton.querySelector(".avatar-mini").textContent = perfil.avatar;
      boton.querySelector(".avatar-mini").style.background = perfil.color;
      boton.querySelector("span:nth-child(2)").textContent = perfil.nombre;
      return perfil;
    } catch (e) {
      if (e.codigo !== 401 && e.codigo !== 428) mostrarError(e);
      return null;
    }
  }

  async function cerrarSesion() {
    const ok = await Avisos.confirmarDoble({
      accion: "cerrar sesión", que: "",
      consecuencia: "Saldrás de tu cuenta en este equipo. Tus perfiles, estrellas y comentarios quedan guardados.",
    });
    if (!ok) return;
    try {
      await api("/cuenta/salir", {});
      sessionStorage.setItem("sf_mensaje", "Cerraste sesión. ¡Vuelve pronto!");
      location.href = "ingresar.html";
    } catch (e) { mostrarError(e); }
  }

  return { pintar, cerrarSesion, logo };
})();
