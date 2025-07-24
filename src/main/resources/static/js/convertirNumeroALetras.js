document.addEventListener("DOMContentLoaded", function () {
    const inputImporte = document.getElementById("importe");
    const inputImporteLetras = document.getElementById("importeLetras");

    // Formatear al cargar si ya tiene valor
    formatCurrency(inputImporte);
    convertirImporteALetras();

    inputImporte.addEventListener("focus", function () {
        unformatCurrency(inputImporte);
    });

    inputImporte.addEventListener("blur", function () {
        formatCurrency(inputImporte);
        convertirImporteALetras();
    });

	inputImporte.addEventListener("input", function () {
	    convertirImporteALetras(); // ✅ Solo convierte sin formatear
	});

    // Limpiar antes de enviar el formulario
    document.querySelector("form").addEventListener("submit", function () {
        inputImporte.value = inputImporte.value.replace(/[$,]/g, '');
    });

    function convertirImporteALetras() {
        const valor = parseFloat(inputImporte.value.replace(/[$,]/g, ''));
        if (!isNaN(valor)) {
            const parteEntera = Math.floor(valor);
            const parteDecimal = Math.round((valor - parteEntera) * 100);
            const letras = numeroALetras(parteEntera).toUpperCase();
            const centavos = parteDecimal.toString().padStart(2, "0");
            inputImporteLetras.value = `${letras} CON ${centavos}/100 DÓLARES AMERICANOS`;
        } else {
            inputImporteLetras.value = "";
        }
    }

	function formatCurrency(input) {
	    const selectionStart = input.selectionStart;
	    const selectionEnd = input.selectionEnd;

	    let rawValue = input.value.replace(/[^0-9.]/g, '');
	    if (rawValue === '') {
	        input.value = '';
	        return;
	    }

	    let floatVal = parseFloat(rawValue);
	    if (isNaN(floatVal)) {
	        input.value = '';
	        return;
	    }

	    const formatted = floatVal.toLocaleString('en-US', {
	        style: 'currency',
	        currency: 'USD',
	        minimumFractionDigits: 2,
	        maximumFractionDigits: 2
	    });

	    input.value = formatted;

	    // Restaurar el cursor al final para evitar que salte mientras se digita
	    input.setSelectionRange(input.value.length, input.value.length);
	}

    function unformatCurrency(input) {
        input.value = input.value.replace(/[$,]/g, '');
    }

    function numeroALetras(num) {
        const unidades = ["", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"];
        const especiales = ["diez", "once", "doce", "trece", "catorce", "quince"];
        const decenas = ["", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"];
        const centenas = ["", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"];

        if (num === 0) return "cero";
        if (num === 100) return "cien";

        let letras = "";

        if (num >= 1000000) {
            const millones = Math.floor(num / 1000000);
            letras += (millones === 1 ? "un millón" : numeroALetras(millones) + " millones");
            num %= 1000000;
            if (num > 0) letras += " ";
        }

        if (num >= 1000) {
            const miles = Math.floor(num / 1000);
            if (miles === 1) {
                letras += "mil";
            } else {
                letras += numeroALetras(miles) + " mil";
            }
            num %= 1000;
            if (num > 0) letras += " ";
        }

        if (num >= 100) {
            const c = Math.floor(num / 100);
            letras += centenas[c];
            num %= 100;
            if (num > 0) letras += " ";
        }

        if (num >= 10 && num <= 15) {
            letras += especiales[num - 10];
        } else if (num < 10) {
            letras += unidades[num];
        } else {
            const d = Math.floor(num / 10);
            const u = num % 10;
            if (d === 2 && u > 0) {
                letras += "veinti" + unidades[u];
            } else {
                letras += decenas[d];
                if (u > 0) letras += " y " + unidades[u];
            }
        }

        return letras.trim();
    }
});
