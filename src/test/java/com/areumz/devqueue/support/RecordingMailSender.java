package com.areumz.devqueue.support;

import com.areumz.devqueue.exception.MailDeliveryException;
import com.areumz.devqueue.mail.MailSender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 테스트용: 진짜로 보내지 않고 메일을 모아 둔다. setFailing(true)면 발송 실패를 흉내 낸다. */
public class RecordingMailSender implements MailSender {

    public record Sent(String to, String subject, String body) {
    }

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private final List<Sent> attempted = new CopyOnWriteArrayList<>();
    private volatile boolean failing;

    @Override
    public void send(String to, String subject, String body) {
        Sent mail = new Sent(to, subject, body);
        attempted.add(mail);
        if (failing) {
            throw new MailDeliveryException(new IllegalStateException("simulated failure"));
        }
        sent.add(mail);
    }

    /** 성공적으로 보낸 메일 */
    public List<Sent> sent() {
        return List.copyOf(sent);
    }

    /** 실패한 시도까지 포함한 모든 시도 */
    public List<Sent> attempted() {
        return List.copyOf(attempted);
    }

    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public void clear() {
        sent.clear();
        attempted.clear();
        failing = false;
    }

    /** 메일 본문의 링크에서 토큰 원본을 꺼낸다. */
    public static String tokenOf(Sent mail) {
        Matcher matcher = TOKEN.matcher(mail.body());
        if (!matcher.find()) {
            throw new IllegalStateException("메일에 토큰 링크가 없다: " + mail.subject());
        }
        return matcher.group(1);
    }
}
