package cl.duoc.pedidos360.rabbitmq.consumer.support;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


class MessageAckTest {

    private static final long TAG = 1L;

    private MessageAck messageAck;
    private Channel channel;

    @BeforeEach
    void setUp() {
        messageAck = new MessageAck();
        channel = mock(Channel.class);
    }

    @Test
    void ack_confirmaElMensaje() throws IOException {
        messageAck.ack(channel, TAG);

        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void errorNoRecuperable_vaDirectoALaDlqSinReintento() throws IOException {
        messageAck.reject(channel, TAG, false,
                new IllegalArgumentException("dato invalido"), "TEST");

        // requeue = false -> el mensaje sale por el dead-letter exchange
        verify(channel).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void respuesta4xxDelCatalogo_esNoRecuperable() throws IOException {
        messageAck.reject(channel, TAG, false,
                new HttpClientErrorException(HttpStatus.BAD_REQUEST), "TEST");

        verify(channel).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void violacionDeIntegridad_esNoRecuperable() throws IOException {
        messageAck.reject(channel, TAG, false,
                new DataIntegrityViolationException("duplicado"), "TEST");

        verify(channel).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void errorRecuperable_primerFallo_reencolaUnaVez() throws IOException {
        messageAck.reject(channel, TAG, false,
                new RuntimeException("base caida"), "TEST");

        // requeue = true -> un reintento
        verify(channel).basicNack(TAG, false, true);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void errorRecuperable_segundoFallo_vaALaDlq() throws IOException {
        messageAck.reject(channel, TAG, true,
                new RuntimeException("base caida"), "TEST");

        // redelivered = true -> ya se reintentó, requeue = false -> DLQ
        verify(channel).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
    }
}