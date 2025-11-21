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

// List saved card payment methods for a customer
app.get('/stripe/payment-methods', async (req, res) => {
  try {
    const { customerId, uid, email } = req.query;
    let customer = null;

    if (customerId) {
      customer = await stripe.customers.retrieve(customerId);
    } else if (uid && email) {
      customer = await findOrCreateCustomer({ uid, email });
    } else {
      return res.status(400).send('customerId or uid+email required');
    }

    const pms = await stripe.paymentMethods.list({
      customer: customer.id,
      type: 'card'
    });

    const mapped = pms.data.map(pm => ({
      id: pm.id,
      brand: pm.card?.brand,
      last4: pm.card?.last4,
      exp_month: pm.card?.exp_month,
      exp_year: pm.card?.exp_year
    }));

    res.json({
      customerId: customer.id,
      paymentMethods: mapped
    });
  } catch (err) {
    console.error(err);
    res.status(500).send(err.message);
  }
});

const port = process.env.PORT || 4242;
app.listen(port, () => console.log(`Stripe backend listening on ${port}`));
