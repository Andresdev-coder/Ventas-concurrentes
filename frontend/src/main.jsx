import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import "./App.css";

// Monta React dentro del elemento raíz de index.html.
ReactDOM.createRoot(document.getElementById("root")).render(
  // StrictMode ayuda a detectar algunos errores de uso de React durante desarrollo.
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
