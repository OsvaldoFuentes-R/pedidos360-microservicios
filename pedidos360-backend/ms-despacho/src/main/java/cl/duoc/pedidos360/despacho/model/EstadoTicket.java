package cl.duoc.pedidos360.despacho.model;

/** Ciclo de vida de un ticket: solo avanza de a un paso. */
public enum EstadoTicket {
    GENERADO,
    EN_RUTA,
    ENTREGADO;

    public boolean puedePasarA(EstadoTicket destino) {
        return destino != null && destino.ordinal() == this.ordinal() + 1;
    }
}
