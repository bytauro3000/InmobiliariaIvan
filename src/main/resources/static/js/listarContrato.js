$(document).ready(function () {
    var tabla = $('#tablaContratos').DataTable({
        pageLength: 6, // Mostrar solo 6 registros por página
        lengthChange: false, // Oculta el combo "mostrar N registros por página"
        dom: 'rtp', // Oculta la búsqueda integrada de DataTables

        language: {
            zeroRecords: "No se encontraron contratos",
            info: "Mostrando página _PAGE_ de _PAGES_",
            infoEmpty: "No hay contratos disponibles",
            infoFiltered: "(filtrado de _MAX_ contratos totales)",
            paginate: {
                next: "Siguiente",
                previous: "Anterior"
            }
        }
    });

    // 🔍 Filtro personalizado por nombre o apellido del cliente
    $('#filtroNombre').on('keyup', function () {
        tabla.column(2).search(this.value).draw(); // columna de clientes
    });
});

