1. Combatente

Passiva: +30% de resistência a dano.

Skill: "Adrenalina de Combate" – ganha Velocidade 2 e Resistência 2 por 10s; matar inimigos durante o efeito aumenta sua duração.

Utilidade: Pode montar barricadas que podem ser quebradas com tiros do Tacz.

2. Rastreador

Passiva: Destaca loot raro em um raio de 15 blocos.

Skill: "Faro de Caça" – revela inimigos próximas (highlight tipo glowing) por 12s.

Utilidade: Consegue criar "pegadas" dos inimigos que passaram por um local nos últimos 2 minutos (sinais no chão visíveis só pra ele).

3. Engenheiro

Passiva: Conserta armas e ferramentas com menos recursos.

Skill: "Torreta Improvisada" coloca uma sentinela temporária que atira com a arma que você apertar com o botão direito por 30s.

Utilidade: Abre uma HUD que apenas ele consegue abrir podendo craftar itens diversos.

4. Médico

Passiva: Levanta jogadores caídos em 5s (vs. 30s).

Skill: Cura em área (regen + absorption).

Utilidade: Remove níveis de ferimento e recebe recompensas.

5. Caçador de Recompensas

Passiva: Recebe +20% de loot de zumbis.

Skill: "Marca Letal" – marca um inimigo para causar +30% de dano contra ele por 20s (Tem que estar na mesma linha de visão).

Utilidade: Pessoas podem marcar jogadores com recompensas em um mural de recompensas, e caçadores de recompensa podem pegar a missão e receber as recompensas por isso



repositories {
    maven {
        // Add curse maven to repositories
        name = "Curse Maven"
        url = "https://www.cursemaven.com"
        content {
            includeGroup "curse.maven"
        }
    }
}

dependencies {
    // You can see the https://www.cursemaven.com/
    // Choose one of the following three

    // If you want to use version tacz-1.20.1-1.1.6-release
    implementation fg.deobf("curse.maven:timeless-and-classics-zero-1028108:6632240-sources-6633203")
}