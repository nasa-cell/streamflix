// Entrada: al tocar se reproduce el video con los personajes (horizontal en PC y tablet,
// vertical en celular). Si el video no carga, se muestra la animación E3 de antes.
// Empieza con un toque porque el navegador no deja sonar música sin uno.
(() => {
  const colores = ["var(--c1)", "var(--c2)", "var(--c3)", "var(--c4)", "var(--c5)"];
  const escena = document.getElementById("escena");
  let empezo = false;
  let destino = "ingresar.html";

  document.getElementById("letrero").innerHTML = [..."StreamFlix"]
    .map((l, i) => `<b style="--c:${colores[i % 5]};--d:${i * 0.06}s">${l}</b>`).join("");
  document.getElementById("ondas").innerHTML = [0, 1, 2, 3]
    .map(i => `<i class="onda" style="--c:${colores[i]};--d:${1.1 + i * 0.2}s"></i>`).join("");
  document.getElementById("estrellas").innerHTML = Array.from({ length: 90 },
    () => `<i style="left:${Math.random() * 100}%;top:${Math.random() * 100}%;animation-delay:${Math.random() * 2.4}s"></i>`).join("");

  const botonSonido = document.getElementById("sonido");
  const video = document.getElementById("video");
  const pintar = () => { botonSonido.textContent = Sonido.activo ? "🔊" : "🔇"; video.muted = !Sonido.activo; };
  pintar();
  botonSonido.addEventListener("click", e => { e.stopPropagation(); Sonido.cambiar(); pintar(); });

  const forma = innerHeight > innerWidth ? "v" : "h";
  video.innerHTML = `<source src="video/entrada-${forma}.webm" type="video/webm"><source src="video/entrada-${forma}.mp4" type="video/mp4">`;
  video.load();
  // Copia ampliada y difuminada detrás, para rellenar los lados cuando el video no llena la pantalla.
  const fondoVideo = document.getElementById("video-fondo");
  fondoVideo.innerHTML = video.innerHTML;
  fondoVideo.load();
  // Si la pantalla tiene otra forma que el video, se muestra completo para no cortar el logo.
  const ajustarVideo = () => {
    const pantalla = innerWidth / innerHeight, delVideo = forma === "v" ? 9 / 16 : 16 / 9;
    const diferencia = pantalla / delVideo;
    const tope = forma === "h" ? 1.25 : 1.16;
    const llena = diferencia > 0.86 && diferencia < tope;
    video.style.objectFit = llena ? "cover" : "contain";
    fondoVideo.hidden = llena;
  };
  ajustarVideo();
  addEventListener("resize", ajustarVideo);

  api("/cuenta/sesion").then(r => {
    if (r.conectado) destino = r.perfilId ? "inicio.html" : "perfiles.html";
  }).catch(e => mostrarError(e));

  function animacionDeAntes() {
    escena.classList.remove("con-video");
    escena.classList.add("va");
    Sonido.musicaEntrada();
    setTimeout(irse, 4300);
  }

  function empezar() {
    if (empezo) return;
    empezo = true;
    video.muted = !Sonido.activo;
    escena.classList.add("con-video");
    video.play().then(() => { if (!fondoVideo.hidden) fondoVideo.play().catch(() => {}); }).catch(animacionDeAntes);
  }

  video.addEventListener("ended", irse);
  video.addEventListener("error", () => { if (empezo && escena.classList.contains("con-video")) animacionDeAntes(); }, true);

  function irse() {
    document.body.style.transition = "opacity .5s";
    document.body.style.opacity = "0";
    setTimeout(() => { location.href = destino; }, 450);
  }

  escena.addEventListener("click", e => { if (e.target.id !== "saltar") empezar(); });
  document.addEventListener("keydown", e => { if (e.key === "Enter" || e.key === " ") empezar(); });
  document.getElementById("saltar").addEventListener("click", e => { e.stopPropagation(); irse(); });
})();
