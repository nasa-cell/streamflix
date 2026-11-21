(() => {
  const formulario = document.getElementById("formulario");
  const idEditar = Number(parametro("id")) || 0;
  const elegido = { avatar: null, color: null, gustos: [] };
  Campos.preparar(formulario);

  function pintarOpciones(datos) {
    const av = document.getElementById("avatares");
    av.innerHTML = datos.avatares.map(a => `<button type="button" class="avatar-opcion" data-avatar="${a}" aria-label="Dibujo ${a}">${a}</button>`).join("");
    const co = document.getElementById("colores");
    co.innerHTML = datos.colores.map(c => `<button type="button" class="color-opcion" data-color="${c}" style="background:${c}" aria-label="Color ${c}"></button>`).join("");
    const gu = document.getElementById("gustos");
    gu.innerHTML = datos.generos.map((g, i) => `<button type="button" class="chip" data-gusto="${escapar(g)}" style="--g:${COLORES_ACENTO[i % 5]}">${escapar(g)}</button>`).join("");
    elegir("avatar", elegido.avatar || datos.avatares[0]);
    elegir("color", elegido.color || datos.colores[0]);
    elegido.gustos.forEach(g => gu.querySelector(`[data-gusto="${CSS.escape(g)}"]`)?.classList.add("si"));
    contarGustos();
  }

  function elegir(tipo, valor) {
    elegido[tipo] = valor;
    document.querySelectorAll(`[data-${tipo}]`).forEach(b => b.classList.toggle("si", b.dataset[tipo] === valor));
    document.querySelectorAll(".avatar-opcion").forEach(b => { b.style.background = elegido.color || ""; });
  }

  function contarGustos() {
    const n = elegido.gustos.length;
    const t = document.getElementById("cuenta-gustos");
    t.textContent = n < 3 ? `Elegiste ${n}. Te faltan ${3 - n} como mínimo.` : n === 6 ? "Elegiste 6: llegaste al máximo." : `Elegiste ${n}. ¡Bien! (máximo 6)`;
    t.style.color = n < 3 ? "#ffa8a8" : "#8ce99a";
  }

  formulario.addEventListener("click", e => {
    const a = e.target.closest("[data-avatar]");
    const c = e.target.closest("[data-color]");
    const g = e.target.closest("[data-gusto]");
    if (a) elegir("avatar", a.dataset.avatar);
    if (c) elegir("color", c.dataset.color);
    if (g) {
      const v = g.dataset.gusto;
      if (elegido.gustos.includes(v)) elegido.gustos = elegido.gustos.filter(x => x !== v);
      else if (elegido.gustos.length >= 6) { Avisos.mostrar("Máximo 6 gustos.", "info", "Quita uno para elegir otro."); return; }
      else elegido.gustos.push(v);
      g.classList.toggle("si", elegido.gustos.includes(v));
      contarGustos();
    }
  });

  formulario.addEventListener("submit", async e => {
    e.preventDefault();
    if (!Campos.validarTodo(formulario)) return;
    if (elegido.gustos.length < 3) { Avisos.mostrar("Elige al menos 3 gustos.", "error", `Llevas ${elegido.gustos.length}.`); return; }
    const datos = {
      id: idEditar, nombre: document.getElementById("nombre").value.trim(), avatar: elegido.avatar, color: elegido.color,
      infantil: document.getElementById("infantil").value === "si", idioma: document.getElementById("idioma").value, gustos: elegido.gustos,
    };
    const boton = document.getElementById("guardar");
    boton.disabled = true;
    try {
      const r = await api(idEditar ? "/perfiles/editar" : "/perfiles/crear", datos);
      sessionStorage.setItem("sf_mensaje", r.mensaje);
      location.href = "perfiles.html";
    } catch (err) {
      mostrarError(err, formulario);
    } finally { boton.disabled = false; }
  });

  (async () => {
    try {
      const datos = await api("/perfiles/");
      if (idEditar) {
        const { perfil } = await api("/perfiles/uno?id=" + idEditar);
        document.getElementById("titulo").textContent = "Editar perfil de " + perfil.nombre;
        document.getElementById("nombre").value = perfil.nombre;
        document.getElementById("infantil").value = perfil.infantil ? "si" : "no";
        document.getElementById("idioma").value = perfil.idioma;
        elegido.avatar = perfil.avatar;
        elegido.color = perfil.color;
        elegido.gustos = perfil.gustos;
      } else if (datos.perfiles.length >= datos.maximo) {
        Avisos.mostrar(`Ya tienes ${datos.maximo} perfiles, que es el máximo.`, "error", "Borra uno para crear otro.");
        document.getElementById("guardar").disabled = true;
      }
      if (parametro("primero")) document.getElementById("cancelar").classList.add("oculto");
      pintarOpciones(datos);
    } catch (e) { mostrarError(e); }
  })();
})();
