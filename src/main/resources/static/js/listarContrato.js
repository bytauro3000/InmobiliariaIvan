$(document).ready(function () {
    var tabla = $('#tablaContratos').DataTable({
        pageLength: 6, // ✅ Mostrar solo 6 registros por página
        lengthChange: false, // ✅ Oculta el combo "mostrar N registros por página"
        dom: 'rtp', // ✅ Oculta la búsqueda integrada de DataTables (filtro de la derecha)

        language: {
            zeroRecords: "No se encontraron contratos",
            info: "Mostrando página _PAGE_ de _PAGES_",
            infoEmpty: "No hay contratos disponibles",
            infoFiltered: "(filtrado de _MAX_ contratos totales)",
            paginate: {
                next: "Siguiente",
                previous: "Anterior"
            }
        },

        columnDefs: [
            {
                targets: 2, // Columna de clientes
                render: function (data, type, row, meta) {
                    if (type === 'display' || type === 'filter') {
                        const cleanText = $('<div>').html(data).text().replace(/\s+/g, ' ').trim();
                        return cleanText;
                    }
                    return data;
                }
            }
        ]
    });

    // 🔍 Filtro por nombre + apellido del cliente
    $('#filtroNombre').on('keyup', function () {
        tabla.column(2).search(this.value).draw();
    });
});
