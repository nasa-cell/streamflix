(async () => {
  Menu.pintar();
  const caja = document.getElementById("categoria");
  const genero = parametro("genero");
  if (!genero) {
    caja.innerHTML = `<div class="vacio"><b>🎬</b>Falta indicar qué categoría abrir.<br><a href="inicio.html">Volver al inicio</a></div>`;
    return;
  }
  document.title = genero + " · StreamFlix";
  let tipo = "Todo";

  async function pintar(primeraVez) {
    try {
      const q = new URLSearchParams({ tipo, genero });
      const [d, c] = await Promise.all([api("/recomendaciones/explorar?" + q),
        primeraVez ? api("/recomendaciones/categorias") : Promise.resolve(null)]);
      const e = d.explorar;
      if (primeraVez) {
        const cat = c.categorias.find(x => x.nombre === genero);
        if (!cat) {
          caja.innerHTML = `<div class="vacio"><b>😕</b>No hay títulos de «${escapar(genero)}» para este perfil.<br><a href="inicio.html">Volver al inicio</a></div>`;
          return;
        }
        caja.innerHTML = `
          <a class="btn btn-2" href="inicio.html" style="margin-top:18px">← Volver al inicio</a>
          <div class="banner-categoria aparece" style="--m:var(--c5)">
            <div class="mosaico-fotos">${cat.portadas.map(f => imagenDiferida(f)).join("")}</div>
            <div class="mosaico-velo"></div><b>${escapar(genero)}</b>
          </div>
          <div class="fila-filtros" id="tipos-cat" role="group" aria-label="Tipo">${["Todo", "Película", "Serie"].map(t =>
            `<button type="button" class="boton-tipo ${t === tipo ? "si" : ""}" data-tipo="${t}">${{ Todo: "Todo", "Película": "🎬 Películas", Serie: "📺 Series" }[t]}</button>`).join("")}</div>
          <div id="titulos-cat"></div>`;
        caja.querySelectorAll("[data-tipo]").forEach(b => b.addEventListener("click", () => {
          tipo = b.dataset.tipo;
          caja.querySelectorAll("[data-tipo]").forEach(x => x.classList.toggle("si", x === b));
          pintar(false);
        }));
      }
      const zona = document.getElementById("titulos-cat");
      const paraTi = e.filas.find(f => f.id === "para-ti")?.peliculas || [];
      const vistas = e.filas.find(f => f.id === "vistas")?.peliculas || [];
      zona.innerHTML = paraTi.length || vistas.length ? `
        ${paraTi.length ? `<h2 class="titulo-seccion" style="--c:var(--c4)">${escapar(genero)} para ti<small>${paraTi.length} títulos · ordenados por la IA según tus gustos</small></h2>
        <div class="rejilla-titulos">${paraTi.map(x => tarjeta(x, { vistas: e.vistas, conMatch: true })).join("")}</div>` : ""}
        ${vistas.length ? `<h2 class="titulo-seccion" style="--c:var(--c2)">Ya las viste<small>califícalas para mejorar tus recomendaciones</small></h2>
        <div class="rejilla-titulos">${vistas.map(x => tarjeta(x, { vistas: e.vistas })).join("")}</div>` : ""}`
        : `<div class="vacio"><b>😕</b>No hay ${tipo === "Serie" ? "series" : "películas"} de «${escapar(genero)}» por ahora.</div>`;
    } catch (err) {
      mostrarError(err);
      caja.innerHTML = `<div class="vacio"><b>😕</b>${escapar(err.message)}<br><a href="inicio.html">Volver al inicio</a></div>`;
    }
  }
  pintar(true);
})();
