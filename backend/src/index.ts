import express, { Request, Response } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = Number(process.env.PORT) || 3000;
const STORE_API_BASE_URL = process.env.STORE_API_BASE_URL || 'https://fakestoreapi.com';

app.use(cors());
app.use(express.json());

interface Rating {
  rate: number;
  count: number;
}

interface Product {
  id: number;
  title: string;
  price: number;
  description: string;
  category: string;
  image: string;
  rating: Rating;
}

interface ProductRepository {
  getAll(): Promise<Product[]>;
  getByCategory(category: string): Promise<Product[]>;
  getById(id: string): Promise<Product>;
}

class FakeStoreRepository implements ProductRepository {
  async getAll(): Promise<Product[]> {
    const response = await fetch(`${STORE_API_BASE_URL}/products`);
    if (!response.ok) throw new Error(`Upstream error: ${response.status}`);
    return response.json() as Promise<Product[]>;
  }

  async getByCategory(category: string): Promise<Product[]> {
    const response = await fetch(
      `${STORE_API_BASE_URL}/products/category/${encodeURIComponent(category)}`
    );
    if (!response.ok) throw new Error(`Upstream error: ${response.status}`);
    const items = (await response.json()) as Product[];
    return items.sort((a, b) => b.rating.rate - a.rating.rate);
  }

  async getById(id: string): Promise<Product> {
    const response = await fetch(`${STORE_API_BASE_URL}/products/${encodeURIComponent(id)}`);
    if (!response.ok) throw new Error(`Upstream error: ${response.status}`);
    return response.json() as Promise<Product>;
  }
}

interface EmailProvider {
  sendEnquiry(payload: {
    productId: number;
    productTitle: string;
    customerName: string;
    customerEmail: string;
    message?: string;
  }): Promise<void>;
}

class ConsoleEmailProvider implements EmailProvider {
  async sendEnquiry(payload: {
    productId: number;
    productTitle: string;
    customerName: string;
    customerEmail: string;
    message?: string;
  }): Promise<void> {
    console.log('=== SALES INBOX ENQUIRY ===');
    console.log(`To: ${process.env.SALES_INBOX_EMAIL || 'sales@example.com'}`);
    console.log(`Product: [#${payload.productId}] ${payload.productTitle}`);
    console.log(`From: ${payload.customerName} <${payload.customerEmail}>`);
    console.log(`Message: ${payload.message || '(No additional message)'}`);
    console.log('===========================');
  }
}

const productRepo: ProductRepository = new FakeStoreRepository();
const emailProvider: EmailProvider = new ConsoleEmailProvider();

app.get('/api/products/featured', async (_req: Request, res: Response) => {
  try {
    const products = await productRepo.getAll();
    const featured = [...products]
      .sort((a, b) => b.rating.rate - a.rating.rate)
      .slice(0, 5);
    res.json(featured);
  } catch {
    res.status(502).json({ error: 'Unable to load featured products' });
  }
});

app.get('/api/categories', async (_req: Request, res: Response) => {
  try {
    const products = await productRepo.getAll();
    const categories = Array.from(new Set(products.map((p) => p.category)));
    res.json(categories);
  } catch {
    res.status(502).json({ error: 'Unable to load categories' });
  }
});

app.get('/api/categories/:name', async (req: Request, res: Response) => {
  try {
    const products = await productRepo.getByCategory(String(req.params.name));
    res.json(products);
  } catch {
    res.status(502).json({ error: 'Unable to load category products' });
  }
});

app.get('/api/products/search', async (req: Request, res: Response) => {
  try {
    const query = String(req.query.q || '').trim().toLowerCase();
    const products = await productRepo.getAll();
    const results = query
      ? products.filter(
          (p) =>
            p.title.toLowerCase().includes(query) ||
            p.description.toLowerCase().includes(query)
        )
      : products;
    res.json(results);
  } catch {
    res.status(502).json({ error: 'Unable to search products' });
  }
});

app.get('/api/products/:id', async (req: Request, res: Response) => {
  try {
    const product = await productRepo.getById(String(req.params.id));
    res.json(product);
  } catch {
    res.status(502).json({ error: 'Unable to load product details' });
  }
});

app.post('/api/enquiries', async (req: Request, res: Response) => {
  const { productId, productTitle, name, email, message } = req.body;
  if (!name || typeof name !== 'string' || !name.trim()) {
    res.status(400).json({ error: 'Customer name is required' });
    return;
  }
  if (!email || typeof email !== 'string' || !email.includes('@')) {
    res.status(400).json({ error: 'Valid email is required' });
    return;
  }
  await emailProvider.sendEnquiry({
    productId: Number(productId) || 0,
    productTitle: String(productTitle || 'Unknown Product'),
    customerName: name.trim(),
    customerEmail: email.trim(),
    message: message ? String(message).trim() : undefined,
  });
  res.status(200).json({ message: 'Thank you' });
});

app.listen(PORT, () => {
  console.log(`Catalogue backend listening on http://localhost:${PORT}`);
});