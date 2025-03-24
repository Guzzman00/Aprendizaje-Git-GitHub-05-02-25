use std::io;

fn suma_siete_y_verifica_paridad(numero: i32) -> String {
    let resultado = numero + 7;

    if resultado % 2 == 0 {
        format!("{} es par", resultado)
    } else {
        format!("{} es impar", resultado)
    }
}

pub fn main() {
    loop {
        println!("Ingrese un número:");

        let mut input = String::new();

        io::stdin()
            .read_line(&mut input)
            .expect("Fallo al leer línea");

        let numero: i32 = input.trim().parse()
            .expect("Por favor ingrese un número válido");

        println!("{}", suma_siete_y_verifica_paridad(numero));
        println!("¿Desea realizar otra operación? (S/N)");

        let mut continuar = String::new();

        io::stdin()
            .read_line(&mut continuar)
            .expect("Fallo al leer línea");

        let continuar = continuar.trim().to_lowercase();

        if continuar == "s" {
            continue;
        } else {
            println!("Programa finalizado.");
            break;
        }
    }
}