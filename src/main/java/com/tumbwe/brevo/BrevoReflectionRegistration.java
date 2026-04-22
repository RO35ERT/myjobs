package com.tumbwe.brevo;

import brevoModel.CreateSmtpEmail;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(
        targets = {
                SendSmtpEmail.class,
                SendSmtpEmailSender.class,
                SendSmtpEmailTo.class,
                CreateSmtpEmail.class
        },
        registerFullHierarchy = true
)
public class BrevoReflectionRegistration {
}
