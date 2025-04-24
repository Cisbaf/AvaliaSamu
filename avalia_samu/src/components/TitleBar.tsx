export function TitleBar() {
  return (
    <div className="flex flex-row justify-between items-center bg-yellow-100 p-4 text-black shadow-md">
      <h1 className="text-2xl font-bold">SAMU</h1>
      <div className="flex flex-row space-x-4">
        <button className="bg-red-700 hover:bg-red-600 text-white font-bold py-2 px-4 rounded">
          Cadastrar Colaborador
        </button>
      </div>
    </div>
  );
}
