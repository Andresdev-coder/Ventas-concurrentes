import { useCallback, useEffect, useState } from "react";

// URL configurable al compilar; por defecto apunta al backend local.
const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

/** Cliente HTTP pequeño: convierte respuestas JSON en objetos y errores HTTP en excepciones. */
async function api(path, options) {
  const res = await fetch(`${API}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `Error ${res.status}`);
  }
  return res.json();
}

// Traduce los códigos que envía Java a textos legibles en la pantalla.
const ETIQUETA = {
  PENDIENTE: "Pendiente",
  EN_PROCESO: "En proceso",
  COMPLETADA: "Completada",
  RECHAZADA: "Rechazada",
};

export default function App() {
  // useState guarda los datos que cambian; al actualizarlos React vuelve a dibujar la interfaz.
  const [productos, setProductos] = useState([]);
  const [ventas, setVentas] = useState([]);
  const [resumen, setResumen] = useState(null);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ cliente: "", productoId: 1, cantidad: 1 });

  // Polling: pide inventario, ventas y resumen en paralelo cada segundo.
  // useCallback conserva la función entre renderizados para que el efecto no se reinicie.
  const refrescar = useCallback(async () => {
    try {
      const [p, v, r] = await Promise.all([
        api("/api/productos"),
        api("/api/ventas"),
        api("/api/resumen"),
      ]);
      setProductos(p);
      setVentas(v);
      setResumen(r);
      setError((e) => (e.startsWith("No se pudo conectar") ? "" : e));
    } catch {
      setError("No se pudo conectar con el backend. Si esta en Render gratis, puede tardar ~1 min en despertar.");
    }
  }, []);

  // useEffect inicia el refresco al montar la pantalla y limpia el intervalo al desmontarla.
  useEffect(() => {
    refrescar();
    const id = setInterval(refrescar, 1000);
    return () => clearInterval(id);
  }, [refrescar]);

  // Ejecuta una acción de usuario y presenta errores del backend en la interfaz.
  async function accion(fn) {
    try {
      setError("");
      await fn();
      refrescar();
    } catch (e) {
      setError(e.message);
    }
  }

  // Envía el formulario. preventDefault evita que el navegador recargue la página.
  const registrar = (e) => {
    e.preventDefault();
    accion(async () => {
      await api("/api/ventas", {
        method: "POST",
        body: JSON.stringify({
          cliente: form.cliente,
          productoId: Number(form.productoId),
          cantidad: Number(form.cantidad),
        }),
      });
      setForm((f) => ({ ...f, cliente: "" }));
    });
  };

  // Acciones de demostración: generan 20 ventas o solicitan reiniciar los datos de la demo.
  const simular = () => accion(() => api("/api/ventas/simular?cantidad=20", { method: "POST" }));
  const reiniciar = () => accion(() => api("/api/reiniciar", { method: "POST" }));

  return (
    <main className="container">
      {/* Encabezado con una explicación breve del modelo de concurrencia. */}
      <header>
        <h1>Ventas e Inventario Concurrente | Angel Joao</h1>
        <p className="sub">
          Cada venta la atiende un hilo cajero del backend (Spring Boot). Las que llegan con todos
          los cajeros ocupados esperan en cola.
        </p>
      </header>

      {/* El mensaje solo aparece si falló una petición o una acción. */}
      {error && <div className="alerta">{error}</div>}

      {/* Indicadores del pool: activos ejecutan tareas; en cola esperan un trabajador. */}
      {resumen && (
        <section className="grid resumen">
          <Stat titulo="Cajeros activos" valor={`${resumen.cajerosActivos} / ${resumen.cajeros}`} />
          <Stat titulo="En cola" valor={resumen.enCola} />
          <Stat titulo="Completadas" valor={resumen.completadas} />
          <Stat titulo="Rechazadas" valor={resumen.rechazadas} />
          <Stat titulo="Unidades vendidas" valor={resumen.unidadesVendidas} />
          <Stat
            titulo="Inventario consistente"
            valor={resumen.consistente === null ? "En curso…" : resumen.consistente ? "Sí ✔" : "No ✘"}
          />
        </section>
      )}

      {/* Tarjetas de producto: el backend consulta stock actualizado desde PostgreSQL. */}
      <section className="card">
        <h2>Inventario</h2>
        <div className="grid productos">
          {productos.map((p) => (
            <div key={p.id} className="producto">
              <div className="fila">
                <strong>{p.nombre}</strong>
                <span>
                  {p.stock} / {p.stockInicial}
                </span>
              </div>
              <div className="barra">
                <div
                  className={`relleno ${p.stock === 0 ? "vacio" : ""}`}
                  style={{ width: `${(p.stock / p.stockInicial) * 100}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Formulario de venta y controles para experimentar con la concurrencia. */}
      <section className="card">
        <h2>Registrar venta</h2>
        <form className="form" onSubmit={registrar}>
          <input
            placeholder="Cliente"
            value={form.cliente}
            onChange={(e) => setForm({ ...form, cliente: e.target.value })}
            required
          />
          <select value={form.productoId} onChange={(e) => setForm({ ...form, productoId: e.target.value })}>
            {productos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </select>
          <input
            type="number"
            min="1"
            max="100"
            value={form.cantidad}
            onChange={(e) => setForm({ ...form, cantidad: e.target.value })}
          />
          <button type="submit">Vender</button>
        </form>
        <div className="acciones">
          <button className="secundario" onClick={simular}>
            Simular 20 ventas simultáneas
          </button>
          <button className="peligro" onClick={reiniciar}>
            Reiniciar
          </button>
        </div>
      </section>

      {/* Tabla de seguimiento: el polling muestra cómo cada venta cambia de estado. */}
      <section className="card">
        <h2>Ventas (últimas 50)</h2>
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Cliente</th>
                <th>Producto</th>
                <th>Cant.</th>
                <th>Estado</th>
                <th>Hilo</th>
                <th>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {ventas.length === 0 && (
                <tr>
                  <td colSpan="7" className="vacio-tabla">
                    Aún no hay ventas
                  </td>
                </tr>
              )}
              {ventas.map((v) => (
                <tr key={v.id}>
                  <td>{v.id}</td>
                  <td>{v.cliente}</td>
                  <td>{v.producto}</td>
                  <td>{v.cantidad}</td>
                  <td>
                    <span className={`badge ${v.estado}`}>{ETIQUETA[v.estado]}</span>
                  </td>
                  <td>{v.cajero || "—"}</td>
                  <td>{v.mensaje || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  );
}

/** Componente reutilizable para mostrar una métrica del resumen. */
function Stat({ titulo, valor }) {
  return (
    <div className="stat">
      <span className="stat-titulo">{titulo}</span>
      <span className="stat-valor">{valor}</span>
    </div>
  );
}
