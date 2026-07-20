import Navbar from './components/Navbar';
import SubNav from './components/SubNav';
import HeroBanner from './components/HeroBanner';
import ProductGrid from './components/ProductGrid';
import ScrollRow from './components/ScrollRow';
import Footer from './components/Footer';
import {
  categoryCards,
  categoryCards2,
  categoryCards3,
  bestSellers,
  dealsCards,
} from './data/products';
import './App.css';

function App() {
  return (
    <div className="app" id="app">
      {/* Header */}
      <Navbar />
      <SubNav />

      {/* Main Content */}
      <main className="main" id="main-content">
        {/* Hero Banner */}
        <HeroBanner />

        {/* First row of category cards */}
        <ProductGrid cards={categoryCards} />

        {/* Best Sellers - Computers */}
        <ScrollRow section={bestSellers[0]} />

        {/* Second row of category cards */}
        <ProductGrid cards={categoryCards2} />

        {/* Best Sellers - Toys */}
        <ScrollRow section={bestSellers[1]} />

        {/* "Top picks for Republic of Korea" banner */}
        <div className="promo-banner" id="promo-banner">
          <div className="promo-banner__inner">
            <h3 className="promo-banner__title">Top picks for Republic of Korea</h3>
            <p className="promo-banner__subtitle">Discover trending products curated just for you</p>
          </div>
        </div>

        {/* Third row of category cards */}
        <ProductGrid cards={categoryCards3} />

        {/* Best Sellers - Sports */}
        <ScrollRow section={bestSellers[2]} />

        {/* Deals row */}
        <ProductGrid cards={dealsCards} />
      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
}

export default App;
