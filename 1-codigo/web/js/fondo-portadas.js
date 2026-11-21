// En PC y tablet horizontal llena el fondo de los formularios con portadas del catálogo.
// En celular no se agrega nada, para que la página siga liviana y sin scroll.
(() => {
  const ANCHO_MINIMO = 1000;
  const CANTIDAD = 40;
  let puesto = false;

  function poner() {
    if (puesto || innerWidth < ANCHO_MINIMO) return;
    puesto = true;
    const fondo = document.createElement("div");
    fondo.className = "fondo-portadas";
    fondo.setAttribute("aria-hidden", "true");
    let html = "";
    for (let i = 0; i < CANTIDAD; i++) {
      const n = (i * 37) % 146 + 1;
      html += `<img src="portadas/chicas/${n}.jpg" alt="" decoding="async">`;
    }
    fondo.innerHTML = html;
    const velo = document.createElement("div");
    velo.className = "velo-portadas";
    document.body.prepend(velo);
    document.body.prepend(fondo);
    document.body.classList.add("con-fondo");
  }

  poner();
  addEventListener("resize", poner);
})();
