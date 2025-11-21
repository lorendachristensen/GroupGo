 import express from 'express';
  import Stripe from 'stripe';
  import dotenv from 'dotenv';
  dotenv.config();

  const app = express();
  app.use(express.json());

  const stripe = new Stripe(process.env.STRIPE_SECRET_KEY);

  async function findOrCreateCustomer({ uid, email }) {
    const search = await stripe.customers.search({ query: `metadata['uid']:'${uid}'` });
    if (search.data.length) return search.data[0];
    return stripe.customers.create({ email, metadata: { uid } });
  }

  app.post('/stripe/payment-methods', async (req, res) => {
    try {
      const { uid, email } = req.body;
      if (!uid || !email) return res.status(400).send('uid and email required');

      const customer = await findOrCreateCustomer({ uid, email });
      const pms = await stripe.paymentMethods.list({
        customer: customer.id,
        type: 'card',
      });

      const defaultPm = customer.invoice_settings?.default_payment_method || null;
      const cards = pms.data.map((pm) => ({
        id: pm.id,
        brand: pm.card?.brand,
        last4: pm.card?.last4,
        expMonth: pm.card?.exp_month,
        expYear: pm.card?.exp_year,
        isDefault: pm.id === defaultPm,
      }));

      res.json({
        customerId: customer.id,
        defaultPaymentMethod: defaultPm,
        paymentMethods: cards,
      });
    } catch (err) {
      console.error(err);
      res.status(500).send(err.message);
    }
  });

  app.post('/stripe/payment-methods/default', async (req, res) => {
    try {
      const { uid, email, paymentMethodId } = req.body;
      if (!uid || !email || !paymentMethodId) return res.status(400).send('uid, email, and paymentMethodId required');

      const customer = await findOrCreateCustomer({ uid, email });
      await stripe.customers.update(customer.id, {
        invoice_settings: { default_payment_method: paymentMethodId },
      });
      res.json({ ok: true });
    } catch (err) {
      console.error(err);
      res.status(500).send(err.message);
    }
  });

  app.post('/stripe/payment-methods/delete', async (req, res) => {
    try {
      const { uid, email, paymentMethodId } = req.body;
      if (!uid || !email || !paymentMethodId) return res.status(400).send('uid, email, and paymentMethodId required');

      await findOrCreateCustomer({ uid, email }); // ensure customer exists; detach will fail if pm not found
      await stripe.paymentMethods.detach(paymentMethodId);
      res.json({ ok: true });
    } catch (err) {
      console.error(err);
      res.status(500).send(err.message);
    }
  });

  app.post('/stripe/setup-intent', async (req, res) => {
    try {
      const { uid, email } = req.body;
      if (!uid || !email) return res.status(400).send('uid and email required');

      const customer = await findOrCreateCustomer({ uid, email });

      const setupIntent = await stripe.setupIntents.create({
        customer: customer.id,
        payment_method_types: ['card'],
        usage: 'off_session',
      });

      const ephemeralKey = await stripe.ephemeralKeys.create(
        { customer: customer.id },
        { apiVersion: '2023-10-16' }
      );

      res.json({
        customerId: customer.id,
        setupIntentClientSecret: setupIntent.client_secret,
        ephemeralKey: ephemeralKey.secret,
        publishableKey: process.env.STRIPE_PUBLISHABLE_KEY,
      });
    } catch (err) {
      console.error(err);
      res.status(500).send(err.message);
    }
  });

  const port = process.env.PORT || 4242;
  app.listen(port, () => console.log(`Stripe backend listening on ${port}`));
