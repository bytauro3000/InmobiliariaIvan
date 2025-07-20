$(document).ready(function () {

    $('#selectCliente, #selectLote').select2({ width: '100%', allowClear: true });

    $('#selectSeparacion').select2({
        placeholder: 'Seleccione una separación',
        allowClear: true,
        width: '100%',
        minimumInputLength: 1,
        ajax: {
            url: '/contrato/buscar-separaciones',
            dataType: 'json',
            delay: 300,
            data: function (params) {
                return { filtro: params.term };
            },
            processResults: function (data) {
                return {
                    results: data.map(sep => ({
                        id: sep.id,
                        text: sep.text
                    }))
                };
            }
        }
    });

    $('#selectSeparacion').on('change', function () {
        const idSeparacion = $(this).val();
        if (idSeparacion) {
            $('#bloqueClientes, #bloqueLotes').hide();
        } else {
            $('#bloqueClientes, #bloqueLotes').show();
        }
    });

    $('#tipoContrato').on('change', function () {
        toggleCamposFinanciado();
        calcularSaldo();
    });

    $('#montoTotal, #inicial').on('focus', function () {
        unformatCurrency(this);
    }).on('blur', function () {
        formatCurrency(this);
    }).on('input', function () {
        formatCurrency(this);
        calcularSaldo();
    });

    formatCurrency(document.getElementById('montoTotal'));
    formatCurrency(document.getElementById('inicial'));
    calcularSaldo();
    toggleCamposFinanciado();

    $('form').on('submit', function () {
        ['montoTotal', 'inicial', 'saldo'].forEach(id => {
            const input = document.getElementById(id);
            if (input) {
                input.value = input.value.replace(/[$,]/g, '');
            }
        });
    });

});

function toggleCamposFinanciado() {
    const tipo = $('#tipoContrato').val();

    if (tipo === 'FINANCIADO') {
        $('.financiado-only').show();
        $('#bloqueFinanciamiento').insertAfter('#bloqueDinamicoInicio').show();
        $('#bloqueObservaciones').insertAfter('#bloqueFinanciamiento');
        $('#bloqueClienteLote').insertAfter('#bloqueObservaciones');
    } else {
        $('.financiado-only').hide();
        $('#bloqueObservaciones').insertAfter('#bloqueDinamicoInicio');
        $('#bloqueClienteLote').insertAfter('#bloqueObservaciones');
    }
}

function agregarCliente() {
    const select = $('#selectCliente');
    const id = select.val();
    const texto = select.find('option:selected').text();
    if (!id || $('#cliente-' + id).length > 0) return;
    $('#clientesSeleccionados').append(`
        <li id="cliente-${id}" class="list-group-item d-flex justify-content-between align-items-center">
            ${texto}
            <button type="button" class="btn btn-sm btn-danger" onclick="eliminarCliente(${id})">x</button>
        </li>`);
    $('#clientesHiddenInputs').append(`<input type="hidden" name="idClientes" value="${id}" id="input-cliente-${id}">`);
    select.val(null).trigger('change');
}
function eliminarCliente(id) {
    $('#cliente-' + id).remove();
    $('#input-cliente-' + id).remove();
}

function agregarLote() {
    const select = $('#selectLote');
    const id = select.val();
    const texto = select.find('option:selected').text();
    if (!id || $('#lote-' + id).length > 0) return;
    $('#lotesSeleccionados').append(`
        <li id="lote-${id}" class="list-group-item d-flex justify-content-between align-items-center">
            ${texto}
            <button type="button" class="btn btn-sm btn-danger" onclick="eliminarLote(${id})">x</button>
        </li>`);
    $('#lotesHiddenInputs').append(`<input type="hidden" name="idLotes" value="${id}" id="input-lote-${id}">`);
    select.val(null).trigger('change');
}
function eliminarLote(id) {
    $('#lote-' + id).remove();
    $('#input-lote-' + id).remove();
}

function formatCurrency(input) {
    let value = input.value.replace(/[^\d.]/g, '');
    if (value === '') {
        input.value = '';
        return;
    }
    const parts = value.split('.');
    let integerPart = parts[0];
    const decimalPart = parts[1] ? '.' + parts[1].slice(0, 2) : '';
    integerPart = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    input.value = '$' + integerPart + decimalPart;
}

function unformatCurrency(input) {
    input.value = input.value.replace(/[$,]/g, '');
}

function calcularSaldo() {
    const tipoContrato = $('#tipoContrato').val();
    if (tipoContrato === 'CONTADO') {
        $('#saldo').val(formatSaldo(0));
        $('#saldo').removeClass('is-invalid');
        return;
    }

    let total = parseFloat($('#montoTotal').val().replace(/[$,]/g, '')) || 0;
    let inicial = parseFloat($('#inicial').val().replace(/[$,]/g, '')) || 0;
    let saldo = total - inicial;

    $('#saldo').val(formatSaldo(saldo));
    if (saldo < 0) {
        $('#saldo').addClass('is-invalid');
    } else {
        $('#saldo').removeClass('is-invalid');
    }
}

function formatSaldo(saldo) {
    return saldo.toLocaleString('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: (saldo % 1 === 0) ? 0 : 2
    });
}
