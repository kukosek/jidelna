import '../styles.css';
function App({Component, pageProps}) {
	return (
		<div className="bg-gradient-to-b from-indigo-100 to-green-200 min-h-screen w-screen">
			<Component {...pageProps} />
		</div>
	)
}
export default App;
