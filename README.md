# Sistema de Ventas e Inventario Concurrente

Backend: Java 17 + Spring Boot 3 + PostgreSQL (hilos y pool de cajeros)
Frontend: React + Vite (se despliega en Vercel)

## Como funciona la concurrencia
- `VentaService` crea un `ThreadPoolExecutor` fijo con N trabajadores llamados `Cajero-1..N`.
- Cada solicitud aceptada se guarda como `PENDIENTE` y se entrega como tarea a ese pool.
- Si todos los cajeros están ocupados, la tarea espera en `LinkedBlockingQueue`; no se crea un hilo por venta.
- El hilo cajero cambia la venta a `EN_PROCESO`, simula la atención y luego intenta descontar el inventario.
- El `UPDATE productos SET stock = stock - ? WHERE ... AND stock >= ?` es una sola operación atómica en PostgreSQL.
  Si dos cajeros intentan vender las últimas unidades, la base serializa la actualización de esa fila y
  solo permite el descuento que todavía cumple `stock >= cantidad`.
- Al final, la venta queda `COMPLETADA` si se descontó stock o `RECHAZADA` si no había suficiente.
- La API responde `202 Accepted` al encolar la venta; eso significa que fue aceptada para procesarse,
  no que la venta ya se haya completado.
- `volatile` en estado/cajero/mensaje hace visibles en memoria los cambios entre el hilo cajero y los
  hilos HTTP. PostgreSQL conserva esos cambios para que el historial sobreviva al reinicio.
- `/api/resumen` verifica al terminar que (stock inicial - stock actual) == unidades de ventas completadas.

Para estudiar el código, sigue esta ruta: `VentaController.registrar` valida la petición,
`VentaService.registrar` guarda y encola el trabajo, `VentaService.procesar` lo ejecuta en un cajero,
y `InventarioService.descontar` resuelve de forma segura la competencia por el stock.

## Ejecutar en local
Requisito: instalar Docker Desktop y dejarlo iniciado. Docker descargara PostgreSQL, Java/Maven
y sus dependencias la primera vez; no necesitas instalar Java ni Maven para levantar backend/base.

En la raiz del proyecto:

```bash
docker compose up --build
```

Esto levanta PostgreSQL en `localhost:5432` y el backend en `http://localhost:8080`.
La base crea las tablas y carga el inventario inicial automaticamente. Los datos quedan
guardados en el volumen `postgres_data` cuando detienes o reinicias los contenedores.
Para detenerlos: `docker compose down`. Para borrar tambien la base y empezar de cero:
`docker compose down -v` (esto elimina los datos guardados).

Frontend (necesita Node.js 18 o superior y npm):

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173`. No hay que crear `.env` para desarrollo local; la app usa la API
en `localhost:8080`. `package.json` declara las dependencias de React y Vite, y `npm install`
las descarga en `frontend/node_modules`.

## Endpoints
GET  /api/productos
GET  /api/ventas
GET  /api/resumen
POST /api/ventas            {"cliente":"Ana","productoId":1,"cantidad":2}
POST /api/ventas/simular?cantidad=20
POST /api/reiniciar

## Despliegue
Vercel NO ejecuta aplicaciones Java de larga duracion (es serverless), por eso:

1. Backend en Render (o Railway):
   - New Web Service -> repositorio -> Root Directory: backend -> Runtime: Docker
   - Variable de entorno: CORS_ORIGINS = https://TU-APP.vercel.app
   - Al terminar te da una URL: https://tu-backend.onrender.com
2. Frontend en Vercel:
   - Import Project -> Root Directory: frontend (Framework: Vite)
   - Variable de entorno: VITE_API_URL = https://tu-backend.onrender.com
   - Deploy

En Render/Railway configura tambien `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y
`SPRING_DATASOURCE_PASSWORD` con las credenciales de una base PostgreSQL administrada.

Nota: el plan gratis de Render duerme el servicio tras inactividad; la primera
peticion puede tardar cerca de 1 minuto. Abrelo antes de la sustentacion.
