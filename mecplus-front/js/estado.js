// Estado global do front: quem está logado e o que essa pessoa pode fazer
export const estado = {
    sessao: null, // { id, nome, perfil, permissoes[] }

    pode(funcionalidade) {
        return !!this.sessao && this.sessao.permissoes.includes(funcionalidade);
    },
};
